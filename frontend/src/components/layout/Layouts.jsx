import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { Mail, Phone } from 'lucide-react';
import SocialIcon from '../ui/SocialIcon';
import { CORREO_CONTACTO, REDES_SOCIALES, TELEFONO_CONTACTO } from '../../utils/env';
import { saludApi } from '../../api';
import Navbar, { Logo } from './Navbar';
import { useAuth } from '../../context/AuthContext';

/** Indicador del estado del backend consumiendo GET /salud (HU-49). */
export function IndicadorSalud({ compacto = false }) {
  const [salud, setSalud] = useState(null);

  useEffect(() => {
    let activo = true;
    const consultar = () =>
      saludApi
        .estado()
        .then((s) => activo && setSalud(s))
        .catch(() => activo && setSalud({ estado: 'sin_conexion' }));
    consultar();
    const t = setInterval(consultar, 60000);
    return () => {
      activo = false;
      clearInterval(t);
    };
  }, []);

  const estado = salud?.estado;
  const ok = estado && estado !== 'caido' && estado !== 'sin_conexion' && salud?._status === 200;
  const color = !salud ? 'bg-line' : ok ? (estado === 'degradado' ? 'bg-amber-400' : 'bg-emerald-500') : 'bg-brand';
  const texto = !salud ? 'Verificando servicio…' : estado === 'sin_conexion' ? 'Sin conexión con el servidor' : ok ? (estado === 'degradado' ? 'Servicio degradado' : 'Todos los sistemas operativos') : 'Servicio con fallas';

  return (
    <span className="inline-flex items-center gap-2 text-xs text-ink-muted" title={salud ? `Base de datos: ${salud.baseDatos || '—'} · Pagos: ${salud.pasarelaPagos || '—'}` : ''}>
      <span className="relative flex h-2 w-2">
        {ok && <span className={`absolute inline-flex h-full w-full animate-ping rounded-full opacity-50 ${color}`} />}
        <span className={`relative inline-flex h-2 w-2 rounded-full ${color}`} />
      </span>
      {!compacto && texto}
    </span>
  );
}

const COLUMNAS_FOOTER = [
  {
    titulo: 'Descubre',
    enlaces: [
      { to: '/explorar', label: 'Todos los alojamientos' },
      { to: '/destinos', label: 'Destinos populares' },
      { to: '/comparar', label: 'Comparar alojamientos' },
      { to: '/explorar?tipo=CABANA', label: 'Cabañas y naturaleza' },
    ],
  },
  {
    titulo: 'Anfitriones',
    enlaces: [
      { to: '/anfitriones', label: 'Hazte anfitrión' },
      { to: '/registro', label: 'Crear cuenta de anfitrión' },
      { to: '/como-funciona', label: 'Cómo funciona' },
    ],
  },
  {
    titulo: 'Soporte',
    enlaces: [
      { to: '/ayuda', label: 'Centro de ayuda' },
      { to: '/ayuda#preguntas', label: 'Preguntas frecuentes' },
      { to: '/ayuda#cancelaciones', label: 'Cancelaciones y reembolsos' },
    ],
  },
];

function Footer() {
  return (
    <footer className="mt-24 border-t border-line/60 bg-canvas">
      <div className="container-app py-14">
        <div className="mb-12 flex flex-col items-start justify-between gap-6 rounded-[28px] bg-white p-7 shadow-soft sm:p-9 lg:flex-row lg:items-center">
          <div className="flex items-start gap-4">
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-ink text-white">
              <Mail className="h-5 w-5" />
            </span>
            <div>
              <p className="text-[20px] font-semibold tracking-tight">¿Necesitas algo? Comunícate con Marketplace</p>
              <p className="mt-1 text-[15px] text-ink-muted">Nuestro equipo te responde en menos de 24 horas, todos los días.</p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <a href={`mailto:${CORREO_CONTACTO}`} className="inline-flex h-11 items-center gap-2 rounded-full bg-ink px-5 text-[15px] font-medium text-white transition hover:bg-black">
              <Mail className="h-4 w-4" /> {CORREO_CONTACTO}
            </a>
            <a href={`tel:${TELEFONO_CONTACTO.replace(/\s/g, '')}`} className="inline-flex h-11 items-center gap-2 rounded-full border border-line bg-white px-5 text-[15px] font-medium text-ink transition hover:bg-canvas">
              <Phone className="h-4 w-4" /> {TELEFONO_CONTACTO}
            </a>
          </div>
        </div>

        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-[1.4fr_1fr_1fr_1fr]">
          <div>
            <Logo />
            <p className="mt-4 max-w-xs text-[14px] leading-relaxed text-ink-muted">
              Casas, cabañas y apartamentos verificados en toda Colombia, con precios claros y pago seguro.
            </p>
            <div className="mt-5 flex gap-2">
              {REDES_SOCIALES.map((r) => (
                <a
                  key={r.red}
                  href={r.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={`Marketplace en ${r.etiqueta}`}
                  title={r.etiqueta}
                  className="flex h-10 w-10 items-center justify-center rounded-full border border-line bg-white text-ink-soft transition hover:-translate-y-0.5 hover:border-ink hover:text-ink"
                >
                  <SocialIcon red={r.red} className="h-[18px] w-[18px]" />
                </a>
              ))}
            </div>
          </div>
          {COLUMNAS_FOOTER.map((c) => (
            <div key={c.titulo}>
              <p className="text-[13px] font-semibold text-ink">{c.titulo}</p>
              <ul className="mt-4 space-y-2.5">
                {c.enlaces.map((e) => (
                  <li key={e.label}>
                    <Link to={e.to} className="text-[14px] text-ink-muted transition hover:text-ink">
                      {e.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 flex flex-col gap-3 border-t border-line/70 pt-6 text-[13px] text-ink-muted sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} Marketplace de Alojamientos · Proyecto de grado · Ingeniería de Sistemas.</p>
          <IndicadorSalud />
        </div>
      </div>
    </footer>
  );
}

export function ScrollAlInicio() {
  const { pathname, hash } = useLocation();
  useEffect(() => {
    if (hash) {
      // Enlaces a una sección (/ayuda#preguntas): se desplaza a ella cuando ya está en pantalla.
      const t = setTimeout(() => document.getElementById(hash.slice(1))?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 120);
      return () => clearTimeout(t);
    }
    window.scrollTo({ top: 0, behavior: 'auto' });
    return undefined;
  }, [pathname, hash]);
  return null;
}

/** Aviso cuando la sesión terminó (token vencido, revocado o cuenta restringida). */
function AvisoSesion() {
  const { avisoSesion, limpiarAviso } = useAuth();
  if (!avisoSesion) return null;
  return (
    <div className="border-b border-amber-200/70 bg-amber-50">
      <div className="container-app flex items-center justify-between gap-4 py-2.5 text-[13.5px] text-amber-800">
        <p>
          {avisoSesion}{' '}
          <NavLink to="/login" onClick={limpiarAviso} className="font-semibold underline underline-offset-2">
            Iniciar sesión
          </NavLink>
        </p>
        <button type="button" onClick={limpiarAviso} className="text-amber-700 hover:text-amber-900" aria-label="Cerrar aviso">
          ✕
        </button>
      </div>
    </div>
  );
}

export function MainLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <AvisoSesion />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

/** Layout de panel (anfitrión y administración) con navegación lateral. */
export function PanelLayout({ titulo, enlaces }) {
  return (
    <div className="container-app py-8 lg:py-10">
      <div className="flex flex-col gap-8 lg:flex-row">
        <aside className="lg:w-60 lg:shrink-0">
          <p className="mb-3 px-3 text-[12px] font-semibold uppercase tracking-wider text-ink-faint">{titulo}</p>
          <nav className="no-scrollbar -mx-1 flex gap-1 overflow-x-auto px-1 lg:sticky lg:top-24 lg:flex-col" aria-label={titulo}>
            {enlaces.map((e) => (
              <NavLink
                key={e.to}
                to={e.to}
                end={e.end}
                className={({ isActive }) =>
                  `flex shrink-0 items-center gap-3 rounded-2xl px-3 py-2.5 text-[14px] font-medium transition ${
                    isActive ? 'bg-ink text-white shadow-soft' : 'text-ink-soft hover:bg-black/[0.04]'
                  }`
                }
              >
                <e.icono className="h-[18px] w-[18px]" strokeWidth={1.8} />
                {e.label}
              </NavLink>
            ))}
          </nav>
        </aside>
        <section className="min-w-0 flex-1">
          <Outlet />
        </section>
      </div>
    </div>
  );
}
