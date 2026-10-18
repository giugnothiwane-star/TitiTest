import clsx from 'clsx';
import { Lock } from 'lucide-react';
import { useState, type CSSProperties, type PointerEvent } from 'react';
import { demoExpiry } from '../domain/cards';
import type { VirtualCard } from '../domain/schema';
import { useBank } from '../store/bank';
import logoUrl from '../assets/violetbank-logo.webp';
import { LogoMark } from './Logo';

const STARS =
  'radial-gradient(1.2px 1.2px at 12% 22%, #fff 50%, transparent 51%), radial-gradient(1px 1px at 32% 68%, #fff 50%, transparent 51%), radial-gradient(1.4px 1.4px at 58% 18%, #fff 50%, transparent 51%), radial-gradient(1px 1px at 74% 54%, #fff 50%, transparent 51%), radial-gradient(1.2px 1.2px at 88% 84%, #fff 50%, transparent 51%), radial-gradient(1px 1px at 44% 88%, #fff 50%, transparent 51%), radial-gradient(1px 1px at 92% 12%, #fff 50%, transparent 51%)';

/** Fond CSS de la face avant ; réutilisé par les vignettes de carte (accueil, paiement). */
export function cardBackground(card: Pick<VirtualCard, 'color' | 'accent' | 'style'>): string {
  switch (card.style) {
    case 'aurora':
      return `radial-gradient(120% 120% at 0% 0%, ${card.color} 0%, transparent 55%), radial-gradient(120% 120% at 100% 100%, ${card.accent} 0%, transparent 60%), linear-gradient(135deg, #2a1f63, #0b1a2e)`;
    case 'metal':
      return `linear-gradient(135deg, ${card.color} 0%, #2b2b35 45%, ${card.color} 100%), repeating-linear-gradient(90deg, rgba(255,255,255,.04) 0 2px, transparent 2px 4px)`;
    case 'minimal':
      return `linear-gradient(160deg, ${card.color}, ${card.color})`;
    case 'glass':
      return `linear-gradient(135deg, rgba(255,255,255,.34) 0%, rgba(255,255,255,.06) 60%), radial-gradient(90% 90% at 100% 0%, ${card.accent}cc 0%, transparent 70%), linear-gradient(135deg, ${card.color}d9, ${card.accent}b3)`;
    case 'holo':
      return 'linear-gradient(115deg, #ffd1f7 0%, #b7ecff 20%, #c9b8ff 40%, #fff1b8 58%, #a8f0dc 76%, #f1b8ff 100%)';
    case 'neon':
      return `radial-gradient(80% 70% at 0% 0%, ${card.color}aa 0%, transparent 60%), radial-gradient(80% 70% at 100% 100%, ${card.accent}aa 0%, transparent 60%), #0b0618`;
    case 'galaxy':
      return `${STARS}, radial-gradient(70% 90% at 85% 15%, ${card.accent}aa 0%, transparent 60%), radial-gradient(80% 80% at 5% 95%, ${card.color}bb 0%, transparent 60%), linear-gradient(160deg, #0a0420, #040111)`;
    case 'royal':
      return 'radial-gradient(70% 60% at 85% 10%, rgba(232,199,106,.28) 0%, transparent 60%), radial-gradient(90% 80% at 0% 100%, rgba(125,216,255,.22) 0%, transparent 60%), linear-gradient(135deg, #1a0b3d 0%, #3b1a8a 48%, #12062b 100%)';
    default:
      return `linear-gradient(135deg, ${card.color} 0%, ${card.accent} 100%)`;
  }
}

/** Contour lumineux propre à certains designs premium. */
function cardRing(card: Pick<VirtualCard, 'color' | 'accent' | 'style'>): string | undefined {
  switch (card.style) {
    case 'neon':
      return `inset 0 0 0 1.5px ${card.color}, inset 0 0 18px ${card.color}88, 0 0 26px ${card.accent}66`;
    case 'glass':
      return 'inset 0 0 0 1px rgba(255,255,255,.45)';
    case 'royal':
      return 'inset 0 0 0 1.5px #E8C76A, 0 10px 34px rgba(232,199,106,.25)';
    case 'galaxy':
      return `inset 0 0 0 1px rgba(255,255,255,.12), 0 10px 34px ${card.accent}44`;
    default:
      return undefined;
  }
}

const SHINE_STYLES = new Set<VirtualCard['style']>(['holo', 'glass', 'royal', 'metal']);

/** Puce dessinée et symbole sans contact purement décoratifs. */
function Chip() {
  return (
    <svg width="42" height="32" viewBox="0 0 42 32" aria-hidden>
      <rect x="1" y="1" width="40" height="30" rx="6" fill="#E8D9A8" stroke="#BFA867" />
      <path d="M1 11h12m16 0h12M1 21h12m16 0h12M13 1v30m16-30v30M13 11h16v10H13z" stroke="#A88F4E" strokeWidth="1.2" fill="none" />
    </svg>
  );
}

function Contactless() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden className="opacity-80">
      <path d="M8.5 7.5a6 6 0 0 1 0 9M12 5a10 10 0 0 1 0 14M15.5 2.5a14 14 0 0 1 0 19" />
    </svg>
  );
}

export interface CardPreview {
  name: string;
  color: string;
  accent: string;
  style: VirtualCard['style'];
  avatar: string | null;
  holderName: string;
  demoId: string;
  createdAt: string;
  locked: boolean;
  kind?: 'standard' | 'disposable';
}

export function VirtualCardView({ card, flippable = true, className, tilt = true }: { card: CardPreview; flippable?: boolean; className?: string; tilt?: boolean }) {
  const enabled = useBank((s) => s.state.settings.cardAnimations && s.state.settings.animations);
  const [flipped, setFlipped] = useState(false);
  const [rot, setRot] = useState({ x: 0, y: 0 });
  const light = card.style === 'holo' || (card.style === 'minimal' && /^#(?:[c-fC-F])/.test(card.color));

  const onMove = (e: PointerEvent<HTMLDivElement>) => {
    if (!enabled || !tilt || e.pointerType !== 'mouse') return;
    const r = e.currentTarget.getBoundingClientRect();
    setRot({ x: ((e.clientY - r.top) / r.height - 0.5) * -10, y: ((e.clientX - r.left) / r.width - 0.5) * 12 });
  };
  const style: CSSProperties = {
    transform: `perspective(900px) rotateX(${rot.x}deg) rotateY(${(flipped ? 180 : 0) + rot.y}deg)`,
  };

  const face = 'card-face absolute inset-0 overflow-hidden rounded-[22px] p-5 shadow-lift';
  return (
    <div className={clsx('relative aspect-[1.586] w-full max-w-[380px] select-none', className)} onPointerMove={onMove} onPointerLeave={() => setRot({ x: 0, y: 0 })}>
      <div
        className={clsx('card-3d relative h-full w-full', flippable && 'cursor-pointer')}
        style={enabled ? style : { transform: flipped ? 'rotateY(180deg)' : undefined }}
        onClick={() => flippable && setFlipped((f) => !f)}
        onKeyDown={(e) => {
          if (flippable && (e.key === 'Enter' || e.key === ' ')) {
            e.preventDefault();
            setFlipped((f) => !f);
          }
        }}
        role={flippable ? 'button' : undefined}
        tabIndex={flippable ? 0 : undefined}
        aria-label={flippable ? `Carte ${card.name} : ${flipped ? 'afficher le recto' : 'retourner la carte'}` : undefined}
      >
        <div className={clsx(face, light ? 'text-[#101014]' : 'text-white')} style={{ background: cardBackground(card), boxShadow: cardRing(card) }}>
          <div className="pointer-events-none absolute -right-16 -top-20 h-56 w-56 rounded-full bg-white/10 blur-2xl" />
          {card.style === 'royal' && <img src={logoUrl} alt="" aria-hidden draggable={false} className="pointer-events-none absolute -bottom-6 -right-4 h-[78%] rotate-[-8deg] rounded-[28%] opacity-30 mix-blend-screen" />}
          {SHINE_STYLES.has(card.style) && <div aria-hidden className={clsx('card-shine pointer-events-none absolute inset-0', enabled && 'animate-shine')} />}
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-2">
              <LogoMark size={28} />
              <span className="text-sm font-bold opacity-90">{card.name}</span>
            </div>
            <span className="flex items-center gap-1.5">
              {card.kind === 'disposable' && <span className="rounded-md bg-white/20 px-1.5 py-0.5 text-[10px] font-bold tracking-wider">JETABLE</span>}
              <span className="rounded-md border border-current px-1.5 py-0.5 text-[10px] font-black tracking-[.2em] opacity-90">DEMO</span>
            </span>
          </div>
          <div className="mt-[7%] flex items-center gap-3">
            <Chip />
            <Contactless />
            {card.avatar && <span className="ml-auto text-2xl">{card.avatar}</span>}
          </div>
          <div className="absolute inset-x-5 bottom-4">
            <p className="num font-mono text-[clamp(13px,4.2vw,18px)] tracking-[.18em] opacity-95">{card.demoId}</p>
            <div className="mt-1.5 flex items-end justify-between gap-2 text-[11px] uppercase tracking-wider opacity-80">
              <span className="truncate">{card.holderName}</span>
              <span className="shrink-0">Exp. démo {demoExpiry(card)}</span>
            </div>
          </div>
          {card.locked && (
            <div className="absolute inset-0 flex animate-fade flex-col items-center justify-center gap-2 bg-[#101014]/55 backdrop-blur-[2px]">
              <span className="flex h-12 w-12 animate-pop items-center justify-center rounded-full bg-white/15">
                <Lock size={22} className="text-white" aria-hidden />
              </span>
              <span className="text-sm font-bold text-white">Carte verrouillée</span>
            </div>
          )}
        </div>
        <div className={clsx(face, 'text-white')} style={{ background: '#19191F', transform: 'rotateY(180deg)' }}>
          <div className="absolute inset-x-0 top-6 h-10 bg-black/70" />
          <div className="mt-16 flex items-center gap-3">
            <div className="h-8 flex-1 rounded bg-white/85" />
            <span className="rounded bg-white px-2 py-1 font-mono text-sm font-bold text-[#101014]">DEMO</span>
          </div>
          <p className="mt-4 text-[11px] leading-relaxed text-white/70">
            Carte de démonstration sans valeur. Identifiant fictif, non rattaché à un réseau de paiement. Aucun paiement réel n’est possible.
          </p>
          <p className="absolute bottom-4 right-5 text-[10px] font-bold tracking-[.25em] text-white/60">SIMULATION</p>
        </div>
      </div>
    </div>
  );
}
