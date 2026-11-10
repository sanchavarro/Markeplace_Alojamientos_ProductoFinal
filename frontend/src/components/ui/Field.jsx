import { forwardRef, useId, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

/** Envoltura de campo con etiqueta, ayuda y mensaje de error accesibles. */
export function Campo({ label, error, ayuda, children, htmlFor, className = '', opcional }) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={htmlFor} className="label">
          {label}
          {opcional && <span className="ml-1 font-normal text-ink-faint">(opcional)</span>}
        </label>
      )}
      {children}
      {error ? (
        <p id={`${htmlFor}-error`} className="mt-1.5 text-[13px] text-brand" role="alert">
          {error}
        </p>
      ) : ayuda ? (
        <p className="mt-1.5 text-[13px] text-ink-faint">{ayuda}</p>
      ) : null}
    </div>
  );
}

export const Input = forwardRef(function Input({ label, error, ayuda, className = '', opcional, id, tipoClave, ...resto }, ref) {
  const autoId = useId();
  const campoId = id || autoId;
  const [visible, setVisible] = useState(false);
  const esClave = tipoClave || resto.type === 'password';
  return (
    <Campo label={label} error={error} ayuda={ayuda} htmlFor={campoId} className={className} opcional={opcional}>
      <div className="relative">
        <input
          ref={ref}
          id={campoId}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${campoId}-error` : undefined}
          {...resto}
          type={esClave ? (visible ? 'text' : 'password') : resto.type}
          className={`field ${error ? 'field-error' : ''} ${esClave ? 'pr-11' : ''}`}
        />
        {esClave && (
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            className="absolute inset-y-0 right-2 my-auto flex h-8 w-8 items-center justify-center rounded-full text-ink-faint hover:bg-black/5 hover:text-ink"
            aria-label={visible ? 'Ocultar clave' : 'Mostrar clave'}
          >
            {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        )}
      </div>
    </Campo>
  );
});

export const Textarea = forwardRef(function Textarea({ label, error, ayuda, className = '', opcional, id, rows = 4, maxLength, value, ...resto }, ref) {
  const autoId = useId();
  const campoId = id || autoId;
  return (
    <Campo
      label={label}
      error={error}
      ayuda={ayuda || (maxLength ? `${String(value ?? '').length}/${maxLength}` : undefined)}
      htmlFor={campoId}
      className={className}
      opcional={opcional}
    >
      <textarea
        ref={ref}
        id={campoId}
        rows={rows}
        maxLength={maxLength}
        value={value}
        aria-invalid={Boolean(error)}
        {...resto}
        className={`field resize-y leading-relaxed ${error ? 'field-error' : ''}`}
      />
    </Campo>
  );
});

export const Select = forwardRef(function Select({ label, error, ayuda, className = '', opcional, id, children, ...resto }, ref) {
  const autoId = useId();
  const campoId = id || autoId;
  return (
    <Campo label={label} error={error} ayuda={ayuda} htmlFor={campoId} className={className} opcional={opcional}>
      <select ref={ref} id={campoId} aria-invalid={Boolean(error)} {...resto} className={`field appearance-none bg-[length:16px] pr-10 ${error ? 'field-error' : ''}`}
        style={{
          backgroundImage:
            "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386868B' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E\")",
          backgroundRepeat: 'no-repeat',
          backgroundPosition: 'right 14px center',
        }}
      >
        {children}
      </select>
    </Campo>
  );
});

/** Mensaje de error general de un formulario (respuesta del backend). */
export function ErrorFormulario({ error }) {
  if (!error) return null;
  const mensaje = typeof error === 'string' ? error : error.message;
  const detalles = typeof error === 'object' ? Object.values(error.errores || {}) : [];
  return (
    <div className="rounded-2xl border border-brand/20 bg-brand-soft px-4 py-3 text-sm text-[#A1192C]" role="alert">
      <p className="font-medium">{mensaje}</p>
      {detalles.length > 0 && (
        <ul className="mt-1.5 list-disc space-y-0.5 pl-5">
          {detalles.map((d) => (
            <li key={d}>{d}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
