import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { House, LogOut, Menu, User, CalendarDays, LayoutGrid, Shield, Bell, X, Compass, MapPinned, Lightbulb, KeyRound, LifeBuoy, ChevronDown } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { ROL_LABEL } from '../../utils/constants';
import { iniciales } from '../../utils/format';
import { resolverImagen } from '../../utils/images';
import { Button } from '../ui';
import NotificationBell from './NotificationBell';

/** Navegación general (visible para todos). */
const ENLACES_GENERALES = [
  { to: '/', label: 'Inicio', icono: House, end: true },
  { to: '/explorar', label: 'Explorar', icono: Compass },
  { to: '/destinos', label: 'Destinos', icono: MapPinned },
  { to: '/como-funciona', label: 'Cómo funciona', icono: Lightbulb },
  { to: '/anfitriones', label: 'Hazte anfitrión', icono: KeyRound },
  { to: '/ayuda', label: 'Ayuda', icono: LifeBuoy },
];

/** Accesos propios de cada rol (botón destacado y menú de usuario). */
const ENLACES_POR_ROL = {
  HUESPED: [{ to: '/mis-reservas', label: 'Mis viajes', icono: CalendarDays }],
  ANFITRION: [
    { to: '/anfitrion', label: 'Mis alojamientos', icono: LayoutGrid, end: true },
    { to: '/anfitrion/reservas', label: 'Reservas recibidas', icono: CalendarDays },
  ],
  ADMINISTRADOR: [{ to: '/admin', label: 'Administración', icono: Shield }],
};

export function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2.5" aria-label="Estancia, inicio">
      <span className="flex h-8 w-8 items-center justify-center rounded-[10px] bg-ink">
        <svg viewBox="0 0 64 64" className="h-5 w-5" aria-hidden="true">
          <path d="M18 34 32 21l14 13v13a3 3 0 0 1-3 3h-7V39h-8v11h-7a3 3 0 0 1-3-3Z" fill="#fff" />
        </svg>
      </span>
      <span className="flex flex-col leading-none">
        <span className="text-[18px] font-semibold tracking-tight text-ink">Estancia</span>
        <span className="mt-0.5 text-[10.5px] font-medium uppercase tracking-[0.14em] text-ink-faint">Inicio</span>
      </span>
    </Link>
  );
}

function Avatar({ usuario, className = 'h-8 w-8 text-xs' }) {
  const foto = resolverImagen(usuario?.fotoUrl);
  return foto ? (
    <img src={foto} alt="" className={`${className} rounded-full object-cover`} onError={(e) => (e.currentTarget.style.display = 'none')} />
  ) : (
    <span className={`${className} flex items-center justify-center rounded-full bg-ink font-semibold text-white`}>{iniciales(usuario?.nombre)}</span>
  );
}

export default function Navbar() {
  const { usuario, autenticado, rol, logout } = useAuth();
  const toast = useToast();
  const navegar = useNavigate();
  const [menu, setMenu] = useState(false);
  const [movil, setMovil] = useState(false);
  const [saliendo, setSaliendo] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!menu) return undefined;
    const fuera = (e) => ref.current && !ref.current.contains(e.target) && setMenu(false);
    document.addEventListener('mousedown', fuera);
    return () => document.removeEventListener('mousedown', fuera);
  }, [menu]);

  const salir = async () => {
    setSaliendo(true);
    await logout();
    setSaliendo(false);
    setMenu(false);
    setMovil(false);
    toast.info('Cerraste sesión de forma segura.');
    navegar('/', { replace: true });
  };

  const enlacesRol = autenticado ? ENLACES_POR_ROL[rol] || [] : [];

  const claseEnlace = ({ isActive }) =>
    `rounded-full px-3.5 py-2 text-[14px] font-medium transition ${isActive ? 'bg-black/[0.06] text-ink' : 'text-ink-muted hover:text-ink'}`;

  return (
    <header className="glass sticky top-0 z-40 border-b border-black/[0.06]">
      <div className="container-app flex h-16 items-center justify-between gap-4">
        <Logo />

        <nav className="hidden items-center gap-0.5 xl:flex" aria-label="Principal">
          {ENLACES_GENERALES.map((e) => (
            <NavLink key={e.to} to={e.to} end={e.end} className={claseEnlace}>
              <span className="flex items-center gap-1.5">
                {e.to === '/' && <e.icono className="h-4 w-4" strokeWidth={1.9} />}
                {e.label}
              </span>
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center gap-1.5">
          {enlacesRol[0] && (
            <NavLink
              to={enlacesRol[0].to}
              end={enlacesRol[0].end}
              className={({ isActive }) =>
                `mr-1 hidden items-center gap-1.5 rounded-full px-3.5 py-2 text-[13.5px] font-semibold transition md:flex ${
                  isActive ? 'bg-ink text-white' : 'bg-canvas text-ink hover:bg-[#EBEBEF]'
                }`
              }
            >
              {(() => {
                const Icono = enlacesRol[0].icono;
                return <Icono className="h-4 w-4" strokeWidth={1.9} />;
              })()}
              {enlacesRol[0].label}
            </NavLink>
          )}
          {autenticado ? (
            <>
              <NotificationBell />
              <div className="relative hidden md:block" ref={ref}>
                <button
                  type="button"
                  onClick={() => setMenu((v) => !v)}
                  className="flex items-center gap-2 rounded-full border border-line/80 bg-white py-1 pl-1 pr-3 shadow-pill transition hover:shadow-soft"
                  aria-expanded={menu}
                  aria-label="Menú de usuario"
                >
                  <Avatar usuario={usuario} />
                  <span className="max-w-[120px] truncate text-[13.5px] font-medium">{usuario?.nombre?.split(' ')[0]}</span>
                  <ChevronDown className="h-3.5 w-3.5 text-ink-faint" />
                </button>
                {menu && (
                  <div className="glass absolute right-0 top-12 z-50 w-64 animate-rise-in overflow-hidden rounded-3xl border border-white/70 p-2 shadow-lift">
                    <div className="px-3 pb-3 pt-2">
                      <p className="truncate text-sm font-semibold">{usuario?.nombre}</p>
                      <p className="truncate text-xs text-ink-muted">{usuario?.correo}</p>
                      <p className="mt-1.5 inline-block rounded-full bg-black/[0.05] px-2 py-0.5 text-[11px] font-medium text-ink-soft">{ROL_LABEL[rol]}</p>
                    </div>
                    <div className="h-px bg-line/60" />
                    {enlacesRol.map((e) => (
                      <Link key={e.to} to={e.to} onClick={() => setMenu(false)} className="mt-1 flex items-center gap-2.5 rounded-2xl px-3 py-2.5 text-sm hover:bg-black/[0.04]">
                        <e.icono className="h-4 w-4 text-ink-muted" /> {e.label}
                      </Link>
                    ))}
                    <Link to="/perfil" onClick={() => setMenu(false)} className="mt-1 flex items-center gap-2.5 rounded-2xl px-3 py-2.5 text-sm hover:bg-black/[0.04]">
                      <User className="h-4 w-4 text-ink-muted" /> Mi perfil
                    </Link>
                    <Link to="/notificaciones" onClick={() => setMenu(false)} className="flex items-center gap-2.5 rounded-2xl px-3 py-2.5 text-sm hover:bg-black/[0.04]">
                      <Bell className="h-4 w-4 text-ink-muted" /> Notificaciones
                    </Link>
                    <button type="button" onClick={salir} disabled={saliendo} className="flex w-full items-center gap-2.5 rounded-2xl px-3 py-2.5 text-left text-sm text-brand hover:bg-brand-soft">
                      <LogOut className="h-4 w-4" /> {saliendo ? 'Cerrando sesión…' : 'Cerrar sesión'}
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="hidden items-center gap-1.5 md:flex">
              <Button variante="ghost" tamano="sm" to="/login">
                Iniciar sesión
              </Button>
              <Button tamano="sm" to="/registro">
                Crear cuenta
              </Button>
            </div>
          )}
          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full text-ink-soft hover:bg-black/5 xl:hidden"
            onClick={() => setMovil((v) => !v)}
            aria-label="Abrir menú"
            aria-expanded={movil}
          >
            {movil ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {movil && (
        <div className="animate-fade-in border-t border-black/[0.05] xl:hidden">
          <nav className="container-app grid gap-1 py-3 sm:grid-cols-2" aria-label="Menú móvil">
            {[...ENLACES_GENERALES, ...enlacesRol].map((e) => (
              <NavLink key={e.to} to={e.to} end={e.end} onClick={() => setMovil(false)} className={({ isActive }) => `flex items-center gap-3 rounded-2xl px-3 py-3 text-[15px] ${isActive ? 'bg-black/[0.05] font-medium' : ''}`}>
                <e.icono className="h-[18px] w-[18px] text-ink-muted" /> {e.label}
              </NavLink>
            ))}
            {autenticado ? (
              <>
                <NavLink to="/perfil" onClick={() => setMovil(false)} className="flex items-center gap-3 rounded-2xl px-3 py-3 text-[15px]">
                  <User className="h-[18px] w-[18px] text-ink-muted" /> Mi perfil
                </NavLink>
                <button type="button" onClick={salir} className="flex items-center gap-3 rounded-2xl px-3 py-3 text-left text-[15px] text-brand">
                  <LogOut className="h-[18px] w-[18px]" /> Cerrar sesión
                </button>
              </>
            ) : (
              <div className="flex gap-2 px-1 pt-2 sm:col-span-2 md:hidden">
                <Button variante="outline" completo to="/login" onClick={() => setMovil(false)}>
                  Iniciar sesión
                </Button>
                <Button completo to="/registro" onClick={() => setMovil(false)}>
                  Crear cuenta
                </Button>
              </div>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}
