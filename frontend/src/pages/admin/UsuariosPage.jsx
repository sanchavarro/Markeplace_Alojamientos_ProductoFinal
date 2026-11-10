import { useState } from 'react';
import { Users } from 'lucide-react';
import { adminApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button, Card, Confirmar, EmptyState, ErrorState, EstadoBadge, Input, Paginacion, SectionTitle, Select, Skeleton } from '../../components/ui';
import { ESTADO_USUARIO, ROL_LABEL, ROLES } from '../../utils/constants';
import { fechaLarga } from '../../utils/format';

/** Restringir o reactivar usuarios (HU-44). El backend verifica el estado en cada solicitud. */
export default function UsuariosPage() {
  const { usuario: yo } = useAuth();
  const toast = useToast();
  const [estado, setEstado] = useState('');
  const [pagina, setPagina] = useState(0);
  const [accion, setAccion] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const { data, error, cargando, recargar } = useAsync(() => adminApi.usuarios({ estado, pagina, tamano: 20 }), [estado, pagina]);
  const lista = data?.contenido || [];

  const ejecutar = async () => {
    setEnviando(true);
    try {
      const r = await adminApi.gestionarUsuario(accion.usuario.id, accion.tipo, motivo);
      toast.exito(`${r.nombre} ahora está ${ESTADO_USUARIO[r.estado]?.label.toLowerCase() || r.estado}.`);
      setAccion(null);
      setMotivo('');
      recargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div>
      <SectionTitle
        titulo="Usuarios"
        subtitulo="Controla el acceso a la plataforma."
        accion={
          <Select
            aria-label="Filtrar por estado"
            className="w-52"
            value={estado}
            onChange={(e) => {
              setPagina(0);
              setEstado(e.target.value);
            }}
          >
            <option value="">Todos los estados</option>
            {Object.entries(ESTADO_USUARIO).map(([k, v]) => (
              <option key={k} value={k}>
                {v.label}
              </option>
            ))}
          </Select>
        }
      />
      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando ? (
        <Skeleton className="h-80 w-full rounded-3xl" />
      ) : lista.length === 0 ? (
        <EmptyState icono={Users} titulo="Sin usuarios en este filtro" />
      ) : (
        <Card padding="p-0" className="overflow-x-auto">
          <table className="table-app">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Registro</th>
                <th className="text-right">Acción</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((u) => (
                <tr key={u.id}>
                  <td>
                    <p className="font-medium text-ink">{u.nombre}</p>
                    <p className="text-xs text-ink-faint">
                      #{u.id} · {u.correo}
                    </p>
                  </td>
                  <td>{ROL_LABEL[u.rol] || u.rol}</td>
                  <td>
                    <EstadoBadge estado={u.estado} mapa={ESTADO_USUARIO} />
                  </td>
                  <td className="whitespace-nowrap">{fechaLarga(u.creadoEn)}</td>
                  <td className="text-right">
                    {u.rol === ROLES.ADMIN || u.id === yo?.id ? (
                      <span className="text-xs text-ink-faint">—</span>
                    ) : u.estado === 'RESTRINGIDO' ? (
                      <Button tamano="sm" variante="outline" onClick={() => setAccion({ tipo: 'REACTIVAR', usuario: u })}>
                        Reactivar
                      </Button>
                    ) : (
                      <Button tamano="sm" variante="danger" onClick={() => setAccion({ tipo: 'RESTRINGIR', usuario: u })}>
                        Restringir
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />

      <Confirmar
        abierto={Boolean(accion)}
        alCerrar={() => setAccion(null)}
        alConfirmar={ejecutar}
        cargando={enviando}
        titulo={accion?.tipo === 'RESTRINGIR' ? `Restringir a ${accion?.usuario.nombre}` : `Reactivar a ${accion?.usuario.nombre}`}
        mensaje={accion?.tipo === 'RESTRINGIR' ? 'Perderá el acceso desde su siguiente solicitud.' : 'Podrá volver a iniciar sesión.'}
        textoConfirmar={accion?.tipo === 'RESTRINGIR' ? 'Restringir' : 'Reactivar'}
        variante={accion?.tipo === 'RESTRINGIR' ? 'brand' : 'primary'}
      >
        <Input className="mt-4" label="Motivo" maxLength={300} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Queda registrado en auditoría" />
      </Confirmar>
    </div>
  );
}
