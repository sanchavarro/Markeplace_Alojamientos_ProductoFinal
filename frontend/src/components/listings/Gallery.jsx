import { useCallback, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronLeft, ChevronRight, Grid3X3, X } from 'lucide-react';
import SafeImage from '../ui/SafeImage';

/**
 * Mosaico Bento de 5 fotos estilo Airbnb (HU-15): una foto grande a la izquierda y cuatro en cuadrícula
 * a la derecha, con zoom sutil al pasar el cursor y botón flotante "Mostrar todas las fotos".
 */
export function BentoGallery({ fotos, semilla, titulo }) {
  const [lightbox, setLightbox] = useState(null);
  const cinco = fotos.slice(0, 5);
  while (cinco.length < 5) cinco.push(fotos[cinco.length % Math.max(fotos.length, 1)] || null);

  const celda = (indice, clases) => (
    <button
      type="button"
      key={indice}
      onClick={() => setLightbox(Math.min(indice, fotos.length - 1))}
      className={`group relative block overflow-hidden focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-accent/30 ${clases}`}
      aria-label={`Ver foto ${indice + 1} de ${titulo}`}
    >
      <SafeImage src={cinco[indice]} semilla={semilla + indice} alt={indice === 0 ? titulo : ''} eager={indice === 0} className="h-full w-full" imgClassName="transition-transform duration-700 ease-out group-hover:scale-[1.04]" />
      <span className="absolute inset-0 bg-black/0 transition group-hover:bg-black/[0.08]" />
    </button>
  );

  return (
    <>
      {/* Móvil: carrusel deslizable */}
      <div className="no-scrollbar -mx-5 flex snap-x snap-mandatory overflow-x-auto sm:hidden">
        {fotos.map((f, i) => (
          <button key={`${f}-${i}`} type="button" onClick={() => setLightbox(i)} className="w-full shrink-0 snap-center">
            <SafeImage src={f} semilla={semilla + i} alt={i === 0 ? titulo : ''} className="aspect-[4/3] w-full" />
          </button>
        ))}
      </div>

      {/* Escritorio: Bento grid */}
      <div className="relative hidden h-[min(56vh,520px)] grid-cols-4 grid-rows-2 gap-2 overflow-hidden rounded-3xl sm:grid">
        {celda(0, 'col-span-2 row-span-2')}
        {celda(1, '')}
        {celda(2, '')}
        {celda(3, '')}
        {celda(4, '')}
        <button
          type="button"
          onClick={() => setLightbox(0)}
          className="glass absolute bottom-5 right-5 flex items-center gap-2 rounded-full border border-white/70 px-4 py-2.5 text-[13.5px] font-semibold text-ink shadow-lift transition hover:scale-[1.02] hover:bg-white"
        >
          <Grid3X3 className="h-4 w-4" /> Mostrar todas las fotos
        </button>
      </div>
      <p className="mt-2 text-right text-xs text-ink-faint sm:hidden">
        {fotos.length} foto{fotos.length === 1 ? '' : 's'} · desliza para ver más
      </p>

      {lightbox !== null && <Lightbox fotos={fotos} inicial={lightbox} semilla={semilla} titulo={titulo} alCerrar={() => setLightbox(null)} />}
    </>
  );
}

/** Lightbox a pantalla completa para recorrer la galería en alta resolución (teclado y gestos). */
export function Lightbox({ fotos, inicial = 0, semilla, titulo, alCerrar }) {
  const [i, setI] = useState(inicial);
  const total = fotos.length;
  const inicioX = useRef(null);
  const tira = useRef(null);

  const mover = useCallback((d) => setI((x) => (x + d + total) % total), [total]);

  useEffect(() => {
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const teclas = (e) => {
      if (e.key === 'Escape') alCerrar();
      if (e.key === 'ArrowRight') mover(1);
      if (e.key === 'ArrowLeft') mover(-1);
    };
    window.addEventListener('keydown', teclas);
    return () => {
      document.body.style.overflow = overflow;
      window.removeEventListener('keydown', teclas);
    };
  }, [alCerrar, mover]);

  useEffect(() => {
    tira.current?.children[i]?.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
  }, [i]);

  return createPortal(
    <div className="fixed inset-0 z-[90] flex animate-fade-in flex-col bg-[#0b0b0c]/95 text-white backdrop-blur-md" role="dialog" aria-modal="true" aria-label={`Galería de ${titulo}`}>
      <div className="flex items-center justify-between px-5 py-4 sm:px-8">
        <button type="button" onClick={alCerrar} className="flex items-center gap-2 rounded-full px-3 py-2 text-sm font-medium transition hover:bg-white/10">
          <X className="h-5 w-5" /> Cerrar
        </button>
        <p className="text-sm text-white/70">
          {i + 1} / {total}
        </p>
        <span className="w-20" />
      </div>

      <div
        className="relative flex min-h-0 flex-1 items-center justify-center px-4 sm:px-20"
        onTouchStart={(e) => (inicioX.current = e.touches[0].clientX)}
        onTouchEnd={(e) => {
          const dx = e.changedTouches[0].clientX - (inicioX.current ?? 0);
          if (Math.abs(dx) > 50) mover(dx < 0 ? 1 : -1);
        }}
      >
        <SafeImage key={i} src={fotos[i]} semilla={semilla + i} alt={`${titulo} · foto ${i + 1}`} eager className="h-full max-h-[78vh] w-full max-w-6xl animate-fade-in rounded-2xl bg-transparent" imgClassName="!object-contain" />
        {total > 1 && (
          <>
            <button type="button" onClick={() => mover(-1)} className="absolute left-3 top-1/2 hidden h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full border border-white/20 bg-white/10 backdrop-blur transition hover:bg-white/20 sm:flex" aria-label="Foto anterior">
              <ChevronLeft className="h-6 w-6" />
            </button>
            <button type="button" onClick={() => mover(1)} className="absolute right-3 top-1/2 hidden h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full border border-white/20 bg-white/10 backdrop-blur transition hover:bg-white/20 sm:flex" aria-label="Foto siguiente">
              <ChevronRight className="h-6 w-6" />
            </button>
          </>
        )}
      </div>

      <div ref={tira} className="no-scrollbar flex gap-2 overflow-x-auto px-5 py-4 sm:justify-center">
        {fotos.map((f, idx) => (
          <button key={`${f}-${idx}`} type="button" onClick={() => setI(idx)} className={`shrink-0 overflow-hidden rounded-lg transition ${idx === i ? 'opacity-100 ring-2 ring-white' : 'opacity-50 hover:opacity-80'}`}>
            <SafeImage src={f} semilla={semilla + idx} className="h-14 w-20" />
          </button>
        ))}
      </div>
    </div>,
    document.body,
  );
}
