/**
 * Иконка короны — используется в логотипе и на дамках.
 */

interface CrownIconProps {
  className?: string;
}

export default function CrownIcon({ className }: CrownIconProps) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M4.2 16.4 2.6 6.2l4.9 3.4L12 3l4.5 6.6 4.9-3.4-1.6 10.2H4.2Z" />
      <rect x="4.2" y="17.6" width="15.6" height="2.6" rx="1.1" />
    </svg>
  );
}
