import { Compass, Lock } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui';
import { HOME_POR_ROL, ROL_LABEL } from '../../utils/constants';

function Estado({ codigo, icono: Icono, titulo, texto, accion }) {
  return (
    <div className="container-app flex min-h-[65vh] flex-col items-center justify-center py-16 text-center">
      <span className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-canvas">
        <Icono className="h-7 w-7 text-ink-muted" strokeWidth={1.6} />
      </span>
      <p className="text-sm font-semibold tracking-wider text-ink-faint">{codigo}</p>
      <h1 className="mt-2 text-[36px] font-semibold tracking-tight">{titulo}</h1>
      <p className="mt-3 max-w-md text-[16px] text-ink-muted">{texto}</p>
      <div className="mt-8">{accion}</div>
    </div>
  );
}

export function NotFoundPage() {
  return <Estado codigo="404" icono={Compass} titulo="Esta página no existe" texto="Puede que el enlace esté mal escrito o que el contenido ya no esté disponible." accion={<Button to="/">Ir al inicio</Button>} />;
}

/** HU-03: un rol sin permiso ve esta pantalla (el backend responde 403 en el mismo caso). */
export function ForbiddenPage() {
  const { rol } = useAuth();
  return (
    <Estado
      codigo="403"
      icono={Lock}
      titulo="No tienes acceso a esta sección"
      texto={rol ? `Tu cuenta de ${ROL_LABEL[rol]?.toLowerCase()} no tiene permiso para ver este contenido.` : 'Inicia sesión con una cuenta que tenga permiso.'}
      accion={<Button to={HOME_POR_ROL[rol] || '/'}>Volver a mi inicio</Button>}
    />
  );
}
