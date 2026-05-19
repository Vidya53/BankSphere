import { Spinner } from './Spinner';

const VARIANTS = {
  primary:   'btn-primary',
  secondary: 'btn-secondary',
  ghost:     'btn-ghost',
  danger:    'btn-danger',
};

const SIZES = {
  sm: 'px-4 py-2 text-xs',
  md: '',
  lg: 'px-6 py-3 text-base',
};

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  icon: Icon,
  iconRight: IconRight,
  className = '',
  children,
  ...rest
}) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={`${VARIANTS[variant]} ${SIZES[size]} ${className}`}
    >
      {loading ? <Spinner size={16} className="text-white" /> : Icon && <Icon size={16} />}
      <span>{children}</span>
      {IconRight && !loading && <IconRight size={16} />}
    </button>
  );
}
