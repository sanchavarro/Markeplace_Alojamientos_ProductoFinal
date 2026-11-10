import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { PantallaCarga } from '../components/ui';
import { HOME_POR_ROL } from '../utils/constants';

/**
 * Protección de rutas por rol (HU-03).
 *  - Mientras se rehidrata la sesión muestra un indicador (evita redirigir al login por error al recargar).
 *  - Sin sesión → /login (guardando la ruta de origen para volver después).
 *  - Con un rol no permitido → /403.
 * El backend vuelve a validar cada solicitud (401/403); este guard solo mejora la experiencia.
 */
export default function RoleGuard({ roles }) {
  const { autenticado, rol, cargando } = useAuth();
  const ubicacion = useLocation();

  if (cargando) return <PantallaCarga texto="Verificando tu sesión…" />;

  if (!autenticado) {
    return <Navigate to="/login" replace state={{ desde: `${ubicacion.pathname}${ubicacion.search}` }} />;
  }

  if (roles?.length && !roles.includes(rol)) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}

/** Solo para visitantes (login, registro...). Un usuario con sesión va a su inicio según el rol. */
export function SoloInvitados() {
  const { autenticado, rol, cargando } = useAuth();
  const ubicacion = useLocation();
  if (cargando) return <PantallaCarga />;
  if (autenticado) return <Navigate to={ubicacion.state?.desde || HOME_POR_ROL[rol] || '/'} replace />;
  return <Outlet />;
}
