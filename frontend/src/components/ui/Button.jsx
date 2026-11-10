import { forwardRef } from 'react';
import { Link } from 'react-router-dom';
import Spinner from './Spinner';

const VARIANTES = {
  primary: 'bg-ink text-white hover:bg-black shadow-sm',
  accent: 'bg-accent text-white hover:bg-accent-hover shadow-sm',
  brand: 'bg-brand text-white hover:brightness-95 shadow-sm',
  secondary: 'bg-canvas text-ink hover:bg-[#EBEBEF]',
  outline: 'border border-line bg-white text-ink hover:border-ink/40 hover:bg-canvas/60',
  ghost: 'text-ink-soft hover:bg-black/5 hover:text-ink',
  danger: 'bg-white text-brand border border-brand/30 hover:bg-brand-soft',
  link: 'text-accent hover:underline underline-offset-4 px-0',
};

const TAMANOS = {
  sm: 'h-9 px-3.5 text-[13px] gap-1.5',
  md: 'h-11 px-5 text-[15px] gap-2',
  lg: 'h-[52px] px-7 text-[16px] gap-2',
  icon: 'h-10 w-10 p-0',
};

const Button = forwardRef(function Button(
  { variante = 'primary', tamano = 'md', cargando = false, completo = false, className = '', children, to, icono: Icono, disabled, type = 'button', ...resto },
  ref,
) {
  const clases = [
    'inline-flex select-none items-center justify-center rounded-full font-medium transition-all duration-200',
    'focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-accent/25 active:scale-[0.98]',
    'disabled:pointer-events-none disabled:opacity-45',
    VARIANTES[variante],
    TAMANOS[tamano],
    completo ? 'w-full' : '',
    className,
  ].join(' ');

  const contenido = (
    <>
      {cargando ? <Spinner className="h-4 w-4" /> : Icono ? <Icono className="h-[18px] w-[18px]" strokeWidth={1.8} /> : null}
      {children}
    </>
  );

  if (to) {
    return (
      <Link ref={ref} to={to} className={clases} {...resto}>
        {contenido}
      </Link>
    );
  }
  return (
    <button ref={ref} type={type} className={clases} disabled={disabled || cargando} aria-busy={cargando || undefined} {...resto}>
      {contenido}
    </button>
  );
});

export default Button;
