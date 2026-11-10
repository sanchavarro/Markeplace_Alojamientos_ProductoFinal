import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { LayoutGrid, CalendarDays, Plus, LayoutDashboard, ShieldCheck, Users, MessageSquare, ScrollText, ShieldAlert, Percent, FileDown, Activity } from 'lucide-react';
import { MainLayout, PanelLayout, ScrollAlInicio } from './components/layout/Layouts';
import RoleGuard, { SoloInvitados } from './routes/RoleGuard';
import { PantallaCarga } from './components/ui';
import { ROLES } from './utils/constants';

// Carga diferida por módulo: cada rol descarga solo el código de sus vistas.
const HomePage = lazy(() => import('./pages/public/HomePage'));
const ExplorarPage = lazy(() => import('./pages/public/ExplorarPage'));
const DestinosPage = lazy(() => import('./pages/public/InfoPages').then((m) => ({ default: m.DestinosPage })));
const ComoFuncionaPage = lazy(() => import('./pages/public/InfoPages').then((m) => ({ default: m.ComoFuncionaPage })));
const AnfitrionesPage = lazy(() => import('./pages/public/InfoPages').then((m) => ({ default: m.AnfitrionesPage })));
const AyudaPage = lazy(() => import('./pages/public/InfoPages').then((m) => ({ default: m.AyudaPage })));
const VerificarCorreoPage = lazy(() => import('./pages/public/AuthPages').then((m) => ({ default: m.VerificarCorreoPage })));
const AlojamientoDetailPage = lazy(() => import('./pages/public/AlojamientoDetailPage'));
const ComparePage = lazy(() => import('./pages/public/ComparePage'));
const LoginPage = lazy(() => import('./pages/public/AuthPages').then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('./pages/public/AuthPages').then((m) => ({ default: m.RegisterPage })));
const RecoverPage = lazy(() => import('./pages/public/AuthPages').then((m) => ({ default: m.RecoverPage })));
const ResetPasswordPage = lazy(() => import('./pages/public/AuthPages').then((m) => ({ default: m.ResetPasswordPage })));
const NotFoundPage = lazy(() => import('./pages/public/StatusPages').then((m) => ({ default: m.NotFoundPage })));
const ForbiddenPage = lazy(() => import('./pages/public/StatusPages').then((m) => ({ default: m.ForbiddenPage })));

const PerfilPage = lazy(() => import('./pages/shared/PerfilPage'));
const NotificacionesPage = lazy(() => import('./pages/shared/NotificacionesPage'));
const ReservaDetallePage = lazy(() => import('./pages/shared/ReservaDetallePage'));

const MisReservasPage = lazy(() => import('./pages/guest/MisReservasPage'));

const HostAlojamientosPage = lazy(() => import('./pages/host/HostAlojamientosPage'));
const AlojamientoFormPage = lazy(() => import('./pages/host/AlojamientoFormPage'));
const GestionAlojamientoPage = lazy(() => import('./pages/host/GestionAlojamientoPage'));
const HostReservasPage = lazy(() => import('./pages/host/HostReservasPage'));

const AdminDashboardPage = lazy(() => import('./pages/admin/AdminDashboardPage'));
const ModeracionPage = lazy(() => import('./pages/admin/ModeracionPage'));
const UsuariosPage = lazy(() => import('./pages/admin/UsuariosPage'));
const ResenasAdminPage = lazy(() => import('./pages/admin/ResenasAdminPage'));
const AuditoriaPage = lazy(() => import('./pages/admin/AuditoriaPage'));
const ReportesPage = lazy(() => import('./pages/admin/ReportesPage'));
const ReservasRevisionPage = lazy(() => import('./pages/admin/OperacionesPages').then((m) => ({ default: m.ReservasRevisionPage })));
const TarifasPage = lazy(() => import('./pages/admin/OperacionesPages').then((m) => ({ default: m.TarifasPage })));
const SaludPage = lazy(() => import('./pages/admin/OperacionesPages').then((m) => ({ default: m.SaludPage })));

const ENLACES_ANFITRION = [
  { to: '/anfitrion', label: 'Mis alojamientos', icono: LayoutGrid, end: true },
  { to: '/anfitrion/alojamientos/nuevo', label: 'Nuevo alojamiento', icono: Plus },
  { to: '/anfitrion/reservas', label: 'Reservas', icono: CalendarDays },
];

const ENLACES_ADMIN = [
  { to: '/admin', label: 'Dashboard', icono: LayoutDashboard, end: true },
  { to: '/admin/moderacion', label: 'Moderación', icono: ShieldCheck },
  { to: '/admin/usuarios', label: 'Usuarios', icono: Users },
  { to: '/admin/resenas', label: 'Reseñas', icono: MessageSquare },
  { to: '/admin/revision', label: 'Reservas en revisión', icono: ShieldAlert },
  { to: '/admin/auditoria', label: 'Auditoría', icono: ScrollText },
  { to: '/admin/reportes', label: 'Reportes', icono: FileDown },
  { to: '/admin/tarifas', label: 'Tarifas y tareas', icono: Percent },
  { to: '/admin/salud', label: 'Salud del sistema', icono: Activity },
];

export default function App() {
  return (
    <>
      <ScrollAlInicio />
      <Suspense fallback={<PantallaCarga />}>
        <Routes>
          <Route element={<MainLayout />}>
            {/* Público */}
            <Route index element={<HomePage />} />
            <Route path="explorar" element={<ExplorarPage />} />
            <Route path="destinos" element={<DestinosPage />} />
            <Route path="como-funciona" element={<ComoFuncionaPage />} />
            <Route path="anfitriones" element={<AnfitrionesPage />} />
            <Route path="ayuda" element={<AyudaPage />} />
            <Route path="alojamientos/:id" element={<AlojamientoDetailPage />} />
            <Route path="comparar" element={<ComparePage />} />
            <Route path="403" element={<ForbiddenPage />} />

            {/* Solo visitantes sin sesión */}
            <Route element={<SoloInvitados />}>
              <Route path="login" element={<LoginPage />} />
              <Route path="registro" element={<RegisterPage />} />
              <Route path="recuperar" element={<RecoverPage />} />
              <Route path="verificar-correo" element={<VerificarCorreoPage />} />
            </Route>
            {/* El enlace del correo (app.frontend.url + "/restablecer?token=") debe funcionar con o sin sesión. */}
            <Route path="restablecer" element={<ResetPasswordPage />} />

            {/* Cualquier usuario autenticado */}
            <Route element={<RoleGuard />}>
              <Route path="perfil" element={<PerfilPage />} />
              <Route path="notificaciones" element={<NotificacionesPage />} />
              <Route path="reservas/:id" element={<ReservaDetallePage />} />
            </Route>

            {/* Huésped */}
            <Route element={<RoleGuard roles={[ROLES.HUESPED]} />}>
              <Route path="mis-reservas" element={<MisReservasPage />} />
            </Route>

            {/* Anfitrión */}
            <Route element={<RoleGuard roles={[ROLES.ANFITRION]} />}>
              <Route path="anfitrion" element={<PanelLayout titulo="Anfitrión" enlaces={ENLACES_ANFITRION} />}>
                <Route index element={<HostAlojamientosPage />} />
                <Route path="alojamientos/nuevo" element={<AlojamientoFormPage />} />
                <Route path="alojamientos/:id" element={<GestionAlojamientoPage />} />
                <Route path="alojamientos/:id/editar" element={<AlojamientoFormPage />} />
                <Route path="reservas" element={<HostReservasPage />} />
              </Route>
            </Route>

            {/* Administrador */}
            <Route element={<RoleGuard roles={[ROLES.ADMIN]} />}>
              <Route path="admin" element={<PanelLayout titulo="Administración" enlaces={ENLACES_ADMIN} />}>
                <Route index element={<AdminDashboardPage />} />
                <Route path="moderacion" element={<ModeracionPage />} />
                <Route path="usuarios" element={<UsuariosPage />} />
                <Route path="resenas" element={<ResenasAdminPage />} />
                <Route path="revision" element={<ReservasRevisionPage />} />
                <Route path="auditoria" element={<AuditoriaPage />} />
                <Route path="reportes" element={<ReportesPage />} />
                <Route path="tarifas" element={<TarifasPage />} />
                <Route path="salud" element={<SaludPage />} />
                <Route path="alojamientos/:id/editar" element={<AlojamientoFormPage />} />
              </Route>
            </Route>

            <Route path="inicio" element={<Navigate to="/" replace />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </Suspense>
    </>
  );
}
