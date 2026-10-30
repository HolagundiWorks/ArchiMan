import React from 'react';
import { cn } from '../../lib/cn';

export interface TabItem<T extends string> {
  id: T;
  label: string;
  icon?: React.ReactNode;
  count?: number;
}

export function SegmentedTabs<T extends string>({
  items,
  value,
  onChange,
  className,
  density = 'comfortable',
}: {
  items: TabItem<T>[];
  value: T;
  onChange: (id: T) => void;
  className?: string;
  density?: 'comfortable' | 'compact';
}) {
  return (
    <div
      role="tablist"
      className={cn(
        'flex gap-1 overflow-x-auto p-1 rounded-md bg-surface-container-low border border-outline-variant',
        className
      )}
    >
      {items.map((item) => {
        const active = item.id === value;
        return (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(item.id)}
            className={cn(
              'inline-flex items-center gap-2 whitespace-nowrap font-semibold transition',
              'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary',
              density === 'compact' ? 'h-9 px-3 text-xs rounded-sm' : 'h-10 px-3.5 text-sm rounded-sm',
              active
                ? 'bg-surface-container-lowest text-primary elevation-1'
                : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container'
            )}
          >
            {item.icon}
            <span>{item.label}</span>
            {typeof item.count === 'number' && (
              <span
                className={cn(
                  'min-w-5 h-5 px-1 rounded-full text-[10px] font-bold inline-flex items-center justify-center',
                  active ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container-high'
                )}
              >
                {item.count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
