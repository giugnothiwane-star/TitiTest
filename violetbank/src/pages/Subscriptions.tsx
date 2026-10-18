import clsx from "clsx";
import { Check, Crown, Lock, Sparkles } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import posterUrl from "../assets/vbanque-affiche.webp";
import { Widget } from "../components/Hero";
import { cardBackground } from "../components/VirtualCardView";
import { Button, ConfirmDialog, KeyValue, PageHeader } from "../components/ui";
import { accountBalance } from "../domain/core";
import { formatDate } from "../domain/dates";
import {
  billingAccountId,
  CARD_STYLES,
  PLAN_INFO,
  PLAN_ORDER,
  planAllowsStyle,
  subscribePlan,
  type PlanInfo,
} from "../domain/plans";
import type { Plan } from "../domain/schema";
import { useBankState, useBusy, useMoney } from "../lib/hooks";
import { useBank } from "../store/bank";

/** Prix mensuel affiché, « Gratuit » pour la formule de base. */
function priceLabel(info: PlanInfo, money: (c: number) => string): string {
  return info.price === 0 ? "Gratuit" : `${money(info.price)} / mois`;
}

function PlanCard({
  info,
  current,
  onChoose,
}: {
  info: PlanInfo;
  current: Plan;
  onChoose: (p: Plan) => void;
}) {
  const money = useMoney();
  const active = info.key === current;
  const upgrade = info.rank > PLAN_INFO[current].rank;
  // Designs propres à cette formule (pas ceux hérités des formules inférieures).
  const styles = CARD_STYLES.filter((s) => s.plan === info.key);
  return (
    <article
      className={clsx(
        "relative overflow-hidden rounded-[28px] p-5 text-white shadow-lift",
        active && "ring-4 ring-azure/60",
      )}
      style={{ background: info.gradient }}
      aria-current={active ? "true" : undefined}
    >
      <div className="pointer-events-none absolute -right-14 -top-16 h-48 w-48 rounded-full bg-white/15 blur-2xl" />
      <div className="relative flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="flex items-center gap-1.5 text-[13px] font-semibold uppercase tracking-[.12em] text-white/75">
            {info.key === "nuit" ? (
              <Crown size={14} aria-hidden />
            ) : (
              <Sparkles size={14} aria-hidden />
            )}
            Formule fictive
          </p>
          <h2 className="mt-1 text-2xl font-extrabold tracking-tight">
            {info.name}
          </h2>
          <p className="text-sm text-white/80">{info.tagline}</p>
        </div>
        <p className="shrink-0 text-right">
          <span className="num block text-xl font-extrabold">
            {priceLabel(info, money)}
          </span>
          {info.price > 0 && (
            <span className="text-[11px] text-white/70">argent fictif</span>
          )}
        </p>
      </div>
      <div className="relative mt-4 flex gap-2">
        {styles.map((s) => (
          <span key={s.value} className="w-1/3 max-w-[110px]">
            <span
              className="block aspect-[1.586] w-full rounded-lg shadow-soft ring-1 ring-white/20"
              style={{
                background: cardBackground({
                  color: "#7656F6",
                  accent: "#38BDF8",
                  style: s.value,
                }),
              }}
            />
            <span className="mt-1 block truncate text-[11px] font-semibold text-white/85">
              {s.label}
            </span>
          </span>
        ))}
      </div>
      <ul className="relative mt-4 space-y-1.5 text-sm">
        {info.perks.map((p) => (
          <li key={p} className="flex items-start gap-2">
            <Check
              size={16}
              className="mt-0.5 shrink-0 text-azure"
              aria-hidden
            />
            <span>{p}</span>
          </li>
        ))}
      </ul>
      <div className="relative mt-5">
        {active ? (
          <span className="flex h-12 items-center justify-center gap-2 rounded-full bg-white/20 font-bold">
            <Check size={18} aria-hidden /> Formule actuelle
          </span>
        ) : (
          <button
            type="button"
            onClick={() => onChoose(info.key)}
            className={clsx(
              "h-12 w-full rounded-full font-bold transition active:scale-[.98]",
              upgrade
                ? "bg-white text-[#2B1C6E] hover:bg-white/90"
                : "bg-black/25 text-white hover:bg-black/35",
            )}
          >
            {upgrade ? `Passer à ${info.name}` : `Revenir à ${info.name}`}
          </button>
        )}
      </div>
    </article>
  );
}

export default function Subscriptions() {
  const state = useBankState();
  const act = useBank((s) => s.act);
  const money = useMoney();
  const { busy, run } = useBusy(500);
  const [choice, setChoice] = useState<Plan | null>(null);
  const current = PLAN_INFO[state.profile.plan];
  const billingId = billingAccountId(state);
  const billing = state.accounts.find((a) => a.id === billingId);
  const balance = useMemo(
    () => (billingId ? accountBalance(state, billingId) : 0),
    [state, billingId],
  );
  const target = choice ? PLAN_INFO[choice] : null;
  const unlocked = CARD_STYLES.filter((s) =>
    planAllowsStyle(state.profile.plan, s.value),
  ).length;

  const confirm = () =>
    run(() => {
      if (!choice) return;
      const res = act((s, ctx) => subscribePlan(s, ctx, choice));
      if (res.ok) setChoice(null);
    });

  return (
    <div className="space-y-5">
      <PageHeader
        title="Abonnements"
        subtitle="Formules fictives payées en argent de démonstration. Elles débloquent de nouveaux designs de carte."
        icon={Crown}
      />

      <section className="relative overflow-hidden rounded-[28px] bg-[#0b0618] shadow-lift">
        <img
          src={posterUrl}
          alt="Affiche VBanque : V couronné, application et carte violettes"
          className="mx-auto block aspect-square w-full max-w-[560px] object-cover"
          draggable={false}
        />
        <div className="flex items-center justify-between gap-3 border-t border-white/10 p-5 text-white">
          <div className="min-w-0">
            <p className="text-[13px] font-semibold uppercase tracking-[.12em] text-azure">
              Votre formule
            </p>
            <p className="text-2xl font-extrabold">{current.name}</p>
            <p className="text-sm text-white/75">
              {unlocked} design{unlocked > 1 ? "s" : ""} de carte sur{" "}
              {CARD_STYLES.length} débloqué{unlocked > 1 ? "s" : ""}
            </p>
          </div>
          <Crown size={30} className="shrink-0 text-azure" aria-hidden />
        </div>
      </section>

      {current.price > 0 && (
        <Widget title="Prélèvement fictif">
          <dl className="divide-y divide-line">
            <KeyValue label="Montant" value={priceLabel(current, money)} />
            <KeyValue
              label="Prochaine échéance"
              value={
                state.profile.planRenewsOn
                  ? formatDate(state.profile.planRenewsOn, "long")
                  : "—"
              }
            />
            <KeyValue label="Compte prélevé" value={billing?.name ?? "—"} />
          </dl>
          <p className="mt-3 text-xs text-muted">
            Renouvelé automatiquement chaque mois dans la simulation. Si le
            solde fictif ne suffit pas, retour à la formule Essentiel.
          </p>
        </Widget>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {PLAN_ORDER.map((p) => (
          <PlanCard
            key={p}
            info={PLAN_INFO[p]}
            current={state.profile.plan}
            onChoose={setChoice}
          />
        ))}
      </div>

      <p className="flex items-start gap-2 rounded-2xl bg-raised/70 p-4 text-xs text-muted">
        <Lock size={14} className="mt-0.5 shrink-0" aria-hidden />
        Aucun abonnement réel : les montants sont prélevés sur votre argent
        fictif de démonstration et aucun paiement n’est effectué.{" "}
        <Link to="/cartes" className="font-semibold text-brand">
          Voir mes cartes
        </Link>
      </p>

      <ConfirmDialog
        open={!!target}
        onClose={() => setChoice(null)}
        onConfirm={confirm}
        loading={busy}
        title={
          target
            ? target.price === 0
              ? "Revenir à Essentiel ?"
              : `Passer à ${target.name} ?`
            : ""
        }
        confirmLabel={
          target && target.price > 0
            ? `Payer ${money(target.price)} fictifs`
            : "Confirmer"
        }
      >
        {target && target.price > 0 ? (
          <div className="space-y-2 text-sm">
            <p>
              {money(target.price)} fictifs seront prélevés maintenant sur «{" "}
              {billing?.name ?? "compte principal"} », puis chaque mois dans la
              simulation.
            </p>
            <p
              className={clsx(
                "text-xs",
                balance < target.price
                  ? "font-semibold text-danger"
                  : "text-muted",
              )}
            >
              Solde fictif disponible : {money(balance)}
              {balance < target.price &&
                " — insuffisant, ajoutez des fonds de test."}
            </p>
          </div>
        ) : (
          <p className="text-sm">
            L’abonnement fictif {current.name} sera résilié sans remboursement.
            Vos cartes gardent leur design actuel, mais les designs premium ne
            seront plus disponibles pour de nouvelles cartes.
          </p>
        )}
      </ConfirmDialog>
    </div>
  );
}
