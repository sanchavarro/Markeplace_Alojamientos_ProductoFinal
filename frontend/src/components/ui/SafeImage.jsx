import { useState } from 'react';
import { alFallarImagen, resolverImagen, respaldoPara } from '../../utils/images';

/**
 * <img> con resolución de URL del backend, carga diferida, aparición suave y respaldo automático.
 */
export default function SafeImage({ src, alt = '', semilla = 0, className = '', imgClassName = '', eager = false, ...resto }) {
  const [cargada, setCargada] = useState(false);
  const url = resolverImagen(src) || respaldoPara(semilla);
  return (
    <div className={`relative overflow-hidden bg-[#ECECEF] ${className}`}>
      {!cargada && <div className="skeleton absolute inset-0" aria-hidden="true" />}
      <img
        src={url}
        alt={alt}
        loading={eager ? 'eager' : 'lazy'}
        decoding="async"
        draggable={false}
        onLoad={() => setCargada(true)}
        onError={(e) => {
          alFallarImagen(semilla)(e);
          setCargada(true);
        }}
        className={`h-full w-full object-cover transition-opacity duration-500 ${cargada ? 'opacity-100' : 'opacity-0'} ${imgClassName}`}
        {...resto}
      />
    </div>
  );
}
