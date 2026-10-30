import React from 'react';
import { cn } from '../../lib/cn';

export interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  error?: string;
  multiline?: boolean;
  rows?: number;
}

/** Outlined MD3-style field used across enterprise forms. */
export function TextField({
  label,
  hint,
  error,
  className,
  id,
  multiline,
  rows = 3,
  ...props
}: TextFieldProps) {
  const fieldId = id || props.name || label?.toLowerCase().replace(/\s+/g, '-');
  const shared =
    'w-full rounded-sm border bg-surface-container-lowest px-3 py-2.5 text-sm text-on-surface ' +
    'placeholder:text-on-surface-variant/60 ' +
    'focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary ' +
    'disabled:opacity-50 disabled:bg-surface-container';

  return (
    <label className={cn('flex flex-col gap-1.5', className)}>
      {label && (
        <span className="type-label-sm text-on-surface-variant normal-case tracking-normal font-semibold">
          {label}
        </span>
      )}
      {multiline ? (
        <textarea
          id={fieldId}
          rows={rows}
          className={cn(shared, 'min-h-[5.5rem] resize-y', error ? 'border-error' : 'border-outline-variant')}
          {...(props as React.TextareaHTMLAttributes<HTMLTextAreaElement>)}
        />
      ) : (
        <input
          id={fieldId}
          className={cn(shared, 'h-11', error ? 'border-error' : 'border-outline-variant')}
          {...props}
        />
      )}
      {(error || hint) && (
        <span className={cn('type-label-sm normal-case tracking-normal', error ? 'text-error' : '')}>
          {error || hint}
        </span>
      )}
    </label>
  );
}
