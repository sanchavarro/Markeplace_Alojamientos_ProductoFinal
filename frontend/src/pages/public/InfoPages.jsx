import { Link } from 'react-router-dom';
import { ArrowRight, BadgeDollarSign, CalendarCheck2, Camera, Mail, Phone, ShieldCheck, Clock, Users, Wallet } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui';
import SafeImage from '../../components/ui/SafeImage';
import { CtaAnfitrion, EncabezadoSeccion, Preguntas, SeccionBeneficios, SeccionDestinos, SeccionPasos, SeccionTipos } from '../../components/home/Secciones';
import { PASOS_ANFITRION, PASOS_HUESPED } from '../../utils/contenido';
import { unsplash } from '../../utils/images';
import { CORREO_CONTACTO, TELEFONO_CONTACTO } from '../../utils/env';
import { ROLES } from '../../utils/constants';

function Portada({ antetitulo, titulo, texto, foto, children }) {
  return (
    <section className="container-app pt-8">
      <div className="relative mx-auto max-w-[1240px] overflow-hidden rounded-[32px]">
        <SafeImage src={foto} semilla={4} className="absolute inset-0 h-full w-full" eager />
        <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/40 to-black/10" />
        <div className="relative max-w-2xl px-8 py-16 text-white sm:px-12 sm:py-24">
          <p className="text-[13px] font-semibold uppercase tracking-[0.14em] text-white/75">{antetitulo}</p>
          <h1 className="mt-3 text-[36px] font-semibold leading-[1.05] tracking-tight sm:text-[52px]">{titulo}</h1>
          <p className="mt-4 text-[17px] text-white/85">{texto}</p>
          {children && <div className="mt-8 flex flex-wrap gap-3">{children}</div>}
        </div>
      </div>
    </section>
  );
}

export function DestinosPage() {
  return (
    <div>
      <Portada antetitulo="Destinos" titulo="Descubre Colombia, un espacio a la vez." texto="Playas, montañas, pueblos patrimonio y ciudades vibrantes. Elige un destino y mira lo que hay disponible." foto={unsplash('1613977257363-707ba9348227', 1800)} />
      <SeccionDestinos conEncabezado={false} />
      <SeccionTipos />
    </div>
  );
}

export function ComoFuncionaPage() {
  return (
    <div>
      <Portada antetitulo="Cómo funciona" titulo="Reservar y hospedar, sin complicaciones." texto="Te explicamos paso a paso qué pasa desde que buscas un alojamiento hasta que dejas tu reseña." foto={unsplash('1600607687939-ce8a6c25118c', 1800)}>
        <Button variante="brand" tamano="lg" to="/explorar">
          Empezar a explorar
        </Button>
      </Portada>
      <SeccionPasos antetitulo="Para huéspedes" titulo="Tu viaje en cuatro pasos" pasos={PASOS_HUESPED} />
      <section className="container-app py-6">
        <div className="mx-auto grid max-w-[1240px] gap-5 md:grid-cols-2">
          <div className="rounded-3xl bg-canvas p-8">
            <CalendarCheck2 className="h-7 w-7" strokeWidth={1.6} />
            <h3 className="mt-4 text-[22px] font-semibold">Reserva inmediata</h3>
            <p className="mt-2 text-[15px] leading-relaxed text-ink-muted">Eliges fechas, revisas el total, ingresas tu tarjeta en la pasarela y la reserva queda confirmada al aprobarse el pago.</p>
          </div>
          <div className="rounded-3xl bg-canvas p-8">
            <Users className="h-7 w-7" strokeWidth={1.6} />
            <h3 className="mt-4 text-[22px] font-semibold">Reserva con aprobación</h3>
            <p className="mt-2 text-[15px] leading-relaxed text-ink-muted">Envías una solicitud sin pagar. Cuando el anfitrión la acepta te llega una notificación y completas el pago desde «Mis viajes».</p>
          </div>
        </div>
      </section>
      <SeccionPasos antetitulo="Para anfitriones" titulo="Publica tu espacio" pasos={PASOS_ANFITRION} fondo="bg-canvas mt-10" />
      <SeccionBeneficios />
    </div>
  );
}

export function AnfitrionesPage() {
  const { autenticado, rol } = useAuth();
  const destino = autenticado && rol === ROLES.ANFITRION ? '/anfitrion/alojamientos/nuevo' : '/registro?rol=ANFITRION';
  const ventajas = [
    { icono: BadgeDollarSign, titulo: 'Tú pones el precio', texto: 'Define la tarifa por noche, la limpieza y servicios adicionales con su costo.' },
    { icono: CalendarCheck2, titulo: 'Tu calendario, tus reglas', texto: 'Abre o bloquea fechas cuando quieras y elige reserva inmediata o con aprobación.' },
    { icono: ShieldCheck, titulo: 'Huéspedes verificados', texto: 'Todas las cuentas confirman su correo y cada reserva queda registrada.' },
    { icono: Wallet, titulo: 'Pagos claros', texto: 'El huésped paga por la pasarela antes de llegar; tú ves cada reserva en tu panel.' },
    { icono: Camera, titulo: 'Fotos que venden', texto: 'Sube hasta 20 fotos arrastrándolas; la primera es tu portada.' },
    { icono: Clock, titulo: 'Te avisamos de todo', texto: 'Notificaciones de solicitudes, pagos y cancelaciones en tiempo real.' },
  ];
  return (
    <div>
      <Portada antetitulo="Hazte anfitrión" titulo="Comparte tu espacio y genera ingresos." texto="Publicar es gratis. Tú decides el precio, las fechas y a quién recibir." foto={unsplash('1600585154340-be6161a56a0c', 1800)}>
        <Button variante="brand" tamano="lg" to={destino}>
          {autenticado && rol === ROLES.ANFITRION ? 'Publicar un alojamiento' : 'Crear cuenta de anfitrión'}
        </Button>
      </Portada>
      <section className="container-app py-16">
        <EncabezadoSeccion antetitulo="Por qué publicar con nosotros" titulo="Todo lo que necesitas para hospedar" />
        <div className="mx-auto grid max-w-[1240px] gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {ventajas.map((v) => (
            <div key={v.titulo} className="rounded-3xl border border-line/60 bg-white p-6 shadow-soft">
              <v.icono className="h-6 w-6" strokeWidth={1.6} />
              <h3 className="mt-4 text-[18px] font-semibold">{v.titulo}</h3>
              <p className="mt-1.5 text-[14.5px] leading-relaxed text-ink-muted">{v.texto}</p>
            </div>
          ))}
        </div>
      </section>
      <SeccionPasos antetitulo="El proceso" titulo="De la idea a tu primera reserva" pasos={PASOS_ANFITRION} fondo="bg-canvas" />
    </div>
  );
}

export function AyudaPage() {
  return (
    <div>
      <section className="container-app pb-6 pt-14 text-center">
        <p className="text-[13px] font-semibold uppercase tracking-[0.14em] text-brand">Centro de ayuda</p>
        <h1 className="mx-auto mt-3 max-w-2xl text-[38px] font-semibold leading-tight tracking-tight sm:text-[50px]">¿En qué te podemos ayudar?</h1>
        <p className="mx-auto mt-4 max-w-xl text-[17px] text-ink-muted">Respuestas rápidas sobre reservas, pagos, cancelaciones y cuentas.</p>
      </section>
      <section className="container-app py-8">
        <Preguntas id="preguntas" />
      </section>
      <section id="cancelaciones" className="container-app scroll-mt-24 py-10">
        <div className="mx-auto grid max-w-3xl gap-4 sm:grid-cols-2">
          <div className="rounded-3xl bg-canvas p-6">
            <h3 className="text-[17px] font-semibold">Política flexible</h3>
            <p className="mt-1.5 text-[14.5px] text-ink-muted">Devolución del 100 % si cancelas con 2 o más días de anticipación; 50 % si es con menos.</p>
          </div>
          <div className="rounded-3xl bg-canvas p-6">
            <h3 className="text-[17px] font-semibold">Política estricta</h3>
            <p className="mt-1.5 text-[14.5px] text-ink-muted">Devolución del 50 % si cancelas con 7 o más días de anticipación; sin devolución si es con menos.</p>
          </div>
        </div>
        <p className="mx-auto mt-4 max-w-3xl text-center text-[13px] text-ink-faint">El valor exacto lo ves antes de confirmar la cancelación desde «Mis viajes».</p>
      </section>
      <section className="container-app py-10">
        <div className="mx-auto max-w-3xl rounded-[28px] bg-ink p-8 text-center text-white sm:p-12">
          <h2 className="text-[28px] font-semibold tracking-tight">¿No encontraste lo que buscabas?</h2>
          <p className="mt-2 text-white/75">Escríbenos y te respondemos en menos de 24 horas.</p>
          <div className="mt-7 flex flex-wrap justify-center gap-3">
            <a href={`mailto:${CORREO_CONTACTO}`} className="inline-flex h-12 items-center gap-2 rounded-full bg-white px-6 text-[15px] font-semibold text-ink transition hover:bg-canvas">
              <Mail className="h-4 w-4" /> {CORREO_CONTACTO}
            </a>
            <a href={`tel:${TELEFONO_CONTACTO.replace(/\s/g, '')}`} className="inline-flex h-12 items-center gap-2 rounded-full border border-white/30 px-6 text-[15px] font-medium transition hover:bg-white/10">
              <Phone className="h-4 w-4" /> {TELEFONO_CONTACTO}
            </a>
          </div>
        </div>
        <p className="mt-8 text-center">
          <Link to="/explorar" className="inline-flex items-center gap-1 text-[15px] font-medium text-accent hover:underline">
            Volver a explorar alojamientos <ArrowRight className="h-4 w-4" />
          </Link>
        </p>
      </section>
      <CtaAnfitrion foto={unsplash('1600566753190-17f0baa2a6c3', 1800)} />
    </div>
  );
}
