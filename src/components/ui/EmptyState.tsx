import React from 'react';
import { cn } from '../../lib/cn';
import { Button } from './Button';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export function EmptyState({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center text-center px-6 py-12 rounded-md',
        'border border-dashed border-outline-variant bg-surface-container-low/40',
        className
      )}
    >
      {icon && (
        <div className="mb-3 text-on-surface-variant [&_svg]:w-8 [&_svg]:h-8">{icon}</div>
      )}
      <h3 className="type-title-sm">{title}</h3>
      {description && <p className="type-body-sm mt-1.5 max-w-sm">{description}</p>}
      {actionLabel && onAction && (
        <Button className="mt-4" size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
}
