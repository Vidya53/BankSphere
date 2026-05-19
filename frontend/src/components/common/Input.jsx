import { forwardRef } from 'react';

// Small red asterisk for required-field labels — used by Input, Select, Textarea.
function RequiredMark() {
  return <span className="text-accent-danger ml-0.5" aria-hidden="true">*</span>;
}

export const Input = forwardRef(function Input(
  { label, error, hint, leftIcon: LeftIcon, rightSlot, className = '', id, required, ...rest },
  ref
) {
  const inputId = id || rest.name;
  return (
    <div className={className}>
      {label && (
        <label htmlFor={inputId} className="label">
          {label}{required && <RequiredMark />}
        </label>
      )}
      <div className="relative">
        {LeftIcon && (
          <span className="pointer-events-none absolute inset-y-0 left-3 grid place-items-center text-accent-mute dark:text-ink-400">
            <LeftIcon size={16} />
          </span>
        )}
        <input
          ref={ref}
          id={inputId}
          aria-required={required || undefined}
          aria-invalid={error ? 'true' : undefined}
          {...rest}
          className={`input ${LeftIcon ? 'pl-10' : ''} ${rightSlot ? 'pr-10' : ''} ${error ? 'border-accent-danger focus:ring-accent-danger' : ''}`}
        />
        {rightSlot && (
          <span className="absolute inset-y-0 right-3 grid place-items-center">{rightSlot}</span>
        )}
      </div>
      {error ? (
        <p className="mt-1.5 text-xs text-accent-danger">{error}</p>
      ) : hint ? (
        <p className="mt-1.5 text-xs text-accent-mute dark:text-ink-400">{hint}</p>
      ) : null}
    </div>
  );
});

export const Select = forwardRef(function Select(
  { label, error, hint, className = '', children, id, required, ...rest },
  ref
) {
  const inputId = id || rest.name;
  return (
    <div className={className}>
      {label && (
        <label htmlFor={inputId} className="label">
          {label}{required && <RequiredMark />}
        </label>
      )}
      <select
        ref={ref}
        id={inputId}
        aria-required={required || undefined}
        aria-invalid={error ? 'true' : undefined}
        {...rest}
        className={`input pr-10 appearance-none bg-[url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%23737373%22 stroke-width=%222%22><path d=%22M6 9l6 6 6-6%22/></svg>')] bg-[length:14px_14px] bg-no-repeat bg-[right_14px_center] ${error ? 'border-accent-danger focus:ring-accent-danger' : ''}`}
      >
        {children}
      </select>
      {error ? (
        <p className="mt-1.5 text-xs text-accent-danger">{error}</p>
      ) : hint ? (
        <p className="mt-1.5 text-xs text-accent-mute dark:text-ink-400">{hint}</p>
      ) : null}
    </div>
  );
});

export const Textarea = forwardRef(function Textarea(
  { label, error, hint, className = '', id, required, ...rest },
  ref
) {
  const inputId = id || rest.name;
  return (
    <div className={className}>
      {label && (
        <label htmlFor={inputId} className="label">
          {label}{required && <RequiredMark />}
        </label>
      )}
      <textarea
        ref={ref}
        id={inputId}
        aria-required={required || undefined}
        aria-invalid={error ? 'true' : undefined}
        {...rest}
        className={`input min-h-[88px] resize-y ${error ? 'border-accent-danger focus:ring-accent-danger' : ''}`}
      />
      {error ? (
        <p className="mt-1.5 text-xs text-accent-danger">{error}</p>
      ) : hint ? (
        <p className="mt-1.5 text-xs text-accent-mute dark:text-ink-400">{hint}</p>
      ) : null}
    </div>
  );
});
