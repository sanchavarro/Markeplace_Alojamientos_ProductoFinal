/** Íconos simples de redes sociales (SVG propios, monocromáticos, heredan currentColor). */
const ICONOS = {
  instagram: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="5" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="12" cy="12" r="4.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="17.3" cy="6.7" r="1.1" fill="currentColor" />
    </>
  ),
  facebook: <path d="M13.5 21v-7.5h2.6l.4-3h-3V8.6c0-.9.3-1.5 1.6-1.5h1.6V4.4c-.3 0-1.2-.1-2.3-.1-2.3 0-3.9 1.4-3.9 4v2.2H8v3h2.5V21h3Z" fill="currentColor" />,
  x: <path d="M4 4h4.2l4.1 5.6L17 4h3l-6.3 7.3L20.5 20h-4.2l-4.4-6-5.2 6H3.6l6.9-7.9L4 4Z" fill="currentColor" />,
  tiktok: <path d="M14.5 3h2.8c.2 1.9 1.4 3.4 3.4 3.7v2.8c-1.3 0-2.5-.4-3.4-1v6.6a5.4 5.4 0 1 1-5.4-5.4c.3 0 .6 0 .9.1v2.9a2.6 2.6 0 1 0 1.7 2.4V3Z" fill="currentColor" />,
  youtube: (
    <>
      <rect x="2.5" y="5.5" width="19" height="13" rx="4" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="m10 9.2 5 2.8-5 2.8V9.2Z" fill="currentColor" />
    </>
  ),
};

export default function SocialIcon({ red, className = 'h-5 w-5' }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      {ICONOS[red] || null}
    </svg>
  );
}
