/**
 * Abonnements fictifs VBanque : prélevés dans le grand livre de la simulation,
 * ils débloquent des designs de carte supplémentaires. Aucun paiement réel.
 */
import { addMonths, formatDate, type ISODate } from './dates';
import { accountBalance, buildTx, DomainError, guard, ok, requireActiveAccount, withLog, withNotification, withTx, type Ctx, type OpResult } from './core';
import { formatMoney } from './money';
import type { BankState, CardStyle, Plan, Transaction } from './schema';

export interface PlanInfo {
  key: Plan;
  rank: number;
  name: string;
  tagline: string;
  /** Prix mensuel fictif en centimes. */
  price: number;
  gradient: string;
  perks: readonly string[];
}

export const PLAN_ORDER: readonly Plan[] = ['essentiel', 'amethyste', 'orchidee', 'nuit'];

export const PLAN_INFO: Record<Plan, PlanInfo> = {
  essentiel: {
    key: 'essentiel',
    rank: 0,
    name: 'Essentiel',
    tagline: 'Tout le nécessaire, gratuitement.',
    price: 0,
    gradient: 'linear-gradient(135deg,#7DD8FF,#7656F6 55%,#4C32B8)',
    perks: ['Cartes Dégradé et Uni', 'Comptes, coffres et budgets illimités', 'Prêts fictifs et simulateur'],
  },
  amethyste: {
    key: 'amethyste',
    rank: 1,
    name: 'Améthyste',
    tagline: 'Des cartes qui brillent.',
    price: 399,
    gradient: 'linear-gradient(135deg,#C9A2FF,#8B5CF6 50%,#5B2BB8)',
    perks: ['Cartes Aurore, Verre givré et Holographique', 'Badge Améthyste sur le profil', 'Tout Essentiel inclus'],
  },
  orchidee: {
    key: 'orchidee',
    rank: 2,
    name: 'Orchidée',
    tagline: 'Le néon et le métal.',
    price: 899,
    gradient: 'linear-gradient(135deg,#7DD8FF,#F07AC0 45%,#7656F6)',
    perks: ['Cartes Métal, Néon et Galaxie', 'Badge Orchidée sur le profil', 'Tout Améthyste inclus'],
  },
  nuit: {
    key: 'nuit',
    rank: 3,
    name: 'Nuit royale',
    tagline: 'La carte couronnée.',
    price: 1699,
    gradient: 'linear-gradient(135deg,#2B1C6E,#101014 55%,#38BDF8)',
    perks: ['Carte Royale couronnée exclusive', 'Badge couronne sur le profil', 'Tous les designs débloqués'],
  },
};

export const CARD_STYLES: readonly { value: CardStyle; label: string; plan: Plan }[] = [
  { value: 'gradient', label: 'Dégradé', plan: 'essentiel' },
  { value: 'minimal', label: 'Uni', plan: 'essentiel' },
  { value: 'aurora', label: 'Aurore', plan: 'amethyste' },
  { value: 'glass', label: 'Verre givré', plan: 'amethyste' },
  { value: 'holo', label: 'Holographique', plan: 'amethyste' },
  { value: 'metal', label: 'Métal', plan: 'orchidee' },
  { value: 'neon', label: 'Néon', plan: 'orchidee' },
  { value: 'galaxy', label: 'Galaxie', plan: 'orchidee' },
  { value: 'royal', label: 'Royale', plan: 'nuit' },
];

const STYLE_PLAN = new Map(CARD_STYLES.map((s) => [s.value, s.plan]));

export function styleMinPlan(style: CardStyle): Plan {
  return STYLE_PLAN.get(style) ?? 'essentiel';
}

export function planAllowsStyle(plan: Plan, style: CardStyle): boolean {
  return PLAN_INFO[plan].rank >= PLAN_INFO[styleMinPlan(style)].rank;
}

/** Compte prélevé : compte principal actif, à défaut le premier compte actif. */
export function billingAccountId(state: BankState): string | null {
  const active = state.accounts.filter((a) => !a.archived);
  return (active.find((a) => a.kind === 'main') ?? active[0])?.id ?? null;
}

function feeTx(ctx: Ctx, accountId: string, info: PlanInfo, date?: ISODate): Transaction {
  return buildTx(ctx, {
    type: 'plan_fee',
    accountId,
    amount: -info.price,
    label: `Abonnement VBanque ${info.name} (fictif)`,
    category: 'subscriptions',
    ...(date ? { date: `${date}T08:00` } : {}),
  });
}

/**
 * Change de formule. Une formule payante prélève immédiatement un mois fictif et
 * fixe l'échéance suivante ; le retour à Essentiel est gratuit et sans remboursement.
 */
export function subscribePlan(state: BankState, ctx: Ctx, plan: Plan, accountId?: string): OpResult<Transaction | null> {
  return guard(() => {
    const info = PLAN_INFO[plan];
    if (!info) throw new DomainError('Formule inconnue.');
    if (state.profile.plan === plan) throw new DomainError(`Vous êtes déjà abonné à ${info.name}.`);
    const previous = PLAN_INFO[state.profile.plan];
    if (info.price === 0) {
      let next: BankState = { ...state, profile: { ...state.profile, plan, planRenewsOn: null } };
      next = withLog(next, ctx, 'info', 'Abonnement résilié', `${previous.name} → ${info.name}`);
      next = withNotification(next, ctx, {
        kind: 'system',
        title: 'Formule Essentiel activée',
        body: `Votre abonnement fictif ${previous.name} est résilié. Les cartes existantes gardent leur design.`,
        link: '/abonnements',
      });
      return ok(next, null, 'Formule Essentiel activée.');
    }
    const source = accountId ?? billingAccountId(state);
    if (!source) throw new DomainError('Aucun compte actif pour prélever l’abonnement.');
    const account = requireActiveAccount(state, source);
    const balance = accountBalance(state, account.id);
    if (balance < info.price) {
      throw new DomainError(`Solde insuffisant : ${formatMoney(balance)} disponibles pour ${formatMoney(info.price)} demandés.`);
    }
    const tx = feeTx(ctx, account.id, info);
    let next = withTx(state, tx);
    next = { ...next, profile: { ...next.profile, plan, planRenewsOn: addMonths(ctx.today, 1) } };
    next = withLog(next, ctx, 'success', 'Abonnement souscrit', `${info.name} · ${formatMoney(info.price)}/mois`);
    next = withNotification(next, ctx, {
      kind: 'system',
      title: `Bienvenue dans ${info.name}`,
      body: `${formatMoney(info.price)} fictifs prélevés sur « ${account.name} ». Nouveaux designs de carte débloqués.`,
      link: '/cartes',
    });
    return ok(next, tx, `Abonnement ${info.name} activé.`);
  });
}

/** Nombre maximal de mensualités rattrapées d'un coup (saut d'horloge important). */
const MAX_CATCH_UP = 24;

/**
 * Prélève les mensualités échues (horloge réelle ou simulée). Solde insuffisant :
 * retour automatique à Essentiel, comme une résiliation pour défaut de paiement.
 * Renvoie l'état inchangé (même référence) quand rien n'est dû.
 */
export function chargeDuePlanFees(state: BankState, ctx: Ctx): BankState {
  const info = PLAN_INFO[state.profile.plan];
  let due = state.profile.planRenewsOn;
  if (!info || info.price === 0 || !due || due > ctx.today) return state;
  let next = state;
  for (let i = 0; i < MAX_CATCH_UP && due && due <= ctx.today; i++) {
    const accountId = billingAccountId(next);
    if (!accountId || accountBalance(next, accountId) < info.price) {
      next = { ...next, profile: { ...next.profile, plan: 'essentiel', planRenewsOn: null } };
      next = withLog(next, ctx, 'warning', 'Abonnement suspendu', `Échéance du ${formatDate(due)} impayée`);
      return withNotification(next, ctx, {
        kind: 'system',
        title: 'Abonnement fictif suspendu',
        body: `Solde fictif insuffisant pour ${formatMoney(info.price)} : retour à la formule Essentiel.`,
        link: '/abonnements',
        dedupeKey: `plan-unpaid:${due}`,
      });
    }
    next = withTx(next, feeTx(ctx, accountId, info, due));
    due = addMonths(due, 1);
    next = { ...next, profile: { ...next.profile, planRenewsOn: due } };
  }
  return withNotification(next, ctx, {
    kind: 'transaction',
    title: `Abonnement ${info.name} renouvelé`,
    body: `${formatMoney(info.price)} fictifs prélevés. Prochaine échéance le ${formatDate(due)}.`,
    link: '/abonnements',
    dedupeKey: `plan-renew:${due}`,
  });
}
