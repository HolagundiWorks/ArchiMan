import React from 'react';
import { cn } from '../../lib/cn';

type Tone = 'lowest' | 'low' | 'default' | 'high' | 'highest';
type Elevation = 0 | 1 | 2 | 3;

const toneClass: Record<Tone, string> = {
  lowest: 'bg-surface-container-lowest',
  low: 'bg-surface-container-low',
  default: 'bg-surface-container',
  high: 'bg-surface-container-high',
  highest: 'bg-surface-container-highest',
};

const elevationClass: Record<Elevation, string> = {
  0: '',
  1: 'elevation-1',
  2: 'elevation-2',
  3: 'elevation-3',
};

export interface SurfaceProps extends React.HTMLAttributes<HTMLDivElement> {
  tone?: Tone;
  elevation?: Elevation;
  outlined?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

const padClass = {
  none: '',
  sm: 'p-3',
  md: 'p-4 sm:p-5',
  lg: 'p-5 sm:p-6',
};

/** MD3 surface container — preferred over ad-hoc white cards. */
export function Surface({
  className,
  tone = 'lowest',
  elevation = 0,
  outlined = true,
  padding = 'md',
  ...props
}: SurfaceProps) {
  return (
    <div
      className={cn(
        'rounded-sm text-on-surface',
        toneClass[tone],
        elevationClass[elevation],
        outlined && 'border border-outline-variant',
        padClass[padding],
        className
      )}
      {...props}
    />
  );
}
