export const ArchiCalcEngine = {
  evaluate(expr: string): string {
    try {
      const normalized = this.normalize(expr);
      if (!normalized) return '0.00';
      const val = this.evalSimple(normalized);
      return this.format(val);
    } catch {
      return 'Error';
    }
  },

  eval(expr: string): number {
    try {
      const normalized = this.normalize(expr);
      if (!normalized) return 0.0;
      return this.evalSimple(normalized);
    } catch {
      return 0.0;
    }
  },

  applyFn(value: string, fn: string): string {
    const v = parseFloat(value) || parseFloat(this.evaluate(value));
    if (isNaN(v)) return 'Error';

    let r = v;
    switch (fn) {
      case 'sin':
        r = Math.sin((v * Math.PI) / 180);
        break;
      case 'cos':
        r = Math.cos((v * Math.PI) / 180);
        break;
      case 'tan':
        r = Math.tan((v * Math.PI) / 180);
        break;
      case 'log':
        r = Math.log10(v);
        break;
      case 'ln':
        r = Math.log(v);
        break;
      case 'sqrt':
        r = Math.sqrt(v);
        break;
      case 'x2':
        r = v * v;
        break;
      case '1/x':
        r = 1.0 / v;
        break;
      case '+/-':
        r = -v;
        break;
      default:
        r = v;
    }
    return this.format(r);
  },

  format(result: number): string {
    if (isNaN(result) || !isFinite(result)) return 'Error';
    return result.toFixed(2);
  },

  normalize(expr: string): string {
    return expr
      .replace(/×/g, '*')
      .replace(/÷/g, '/')
      .replace(/−/g, '-')
      .replace(/pi/gi, Math.PI.toString())
      .replace(/%/g, '/100')
      .trim();
  },

  evalSimple(expr: string): number {
    const values: number[] = [];
    const ops: string[] = [];
    let i = 0;

    const precedence = (op: string) => {
      if (op === '+' || op === '-') return 1;
      if (op === '*' || op === '/') return 2;
      if (op === '^') return 3;
      return 0;
    };

    const apply = (op: string) => {
      const b = values.pop() ?? 0;
      const a = values.pop() ?? 0;
      switch (op) {
        case '+':
          values.push(a + b);
          break;
        case '-':
          values.push(a - b);
          break;
        case '*':
          values.push(a * b);
          break;
        case '/':
          values.push(b !== 0 ? a / b : 0);
          break;
        case '^':
          values.push(Math.pow(a, b));
          break;
      }
    };

    while (i < expr.length) {
      const c = expr[i];
      if (/\s/.test(c)) {
        i++;
      } else if (/\d|\./.test(c)) {
        const start = i;
        while (i < expr.length && /[\d.]/.test(expr[i])) i++;
        values.push(parseFloat(expr.substring(start, i)));
      } else if (c === '(') {
        ops.push(c);
        i++;
      } else if (c === ')') {
        while (ops.length && ops[ops.length - 1] !== '(') {
          apply(ops.pop()!);
        }
        if (ops.length && ops[ops.length - 1] === '(') ops.pop();
        i++;
      } else if (['+', '-', '*', '/', '^', 'x'].includes(c)) {
        const op = c === 'x' ? '*' : c;
        if (c === '-' && (i === 0 || ['+', '-', '*', '/', '(', '^', 'x'].includes(expr[i - 1]))) {
          i++;
          const start = i;
          while (i < expr.length && /[\d.]/.test(expr[i])) i++;
          values.push(-parseFloat(expr.substring(start, i)));
        } else {
          while (ops.length && precedence(ops[ops.length - 1]) >= precedence(op)) {
            apply(ops.pop()!);
          }
          ops.push(op);
          i++;
        }
      } else {
        i++;
      }
    }

    while (ops.length) {
      apply(ops.pop()!);
    }

    return values.length ? values[values.length - 1] : 0;
  },
};

export const ArchiCalcMeasure = {
  measure(expr: string) {
    const imperial = expr.includes("'") || expr.includes('"');
    const meters = this.splitFactors(expr).map((f) => this.parseLengthToMeters(f));

    if (meters.length >= 3) {
      const area = meters[0] * meters[1];
      return {
        meters: meters[0],
        squareMeters: area,
        cubicMeters: area * meters[2],
        kind: 'VOLUME',
        imperial,
      };
    } else if (meters.length === 2) {
      return {
        meters: meters[0],
        squareMeters: meters[0] * meters[1],
        cubicMeters: 0,
        kind: 'AREA',
        imperial,
      };
    } else {
      return {
        meters: meters[0] ?? 0,
        squareMeters: 0,
        cubicMeters: 0,
        kind: 'LENGTH',
        imperial,
      };
    }
  },

  formatGrid(expr: string): Record<string, string> {
    const m = this.measure(expr.trim() || '0');
    const n = (v: number) => (isNaN(v) ? '0.00' : v.toFixed(2));
    return {
      m: n(m.meters),
      m2: n(m.squareMeters),
      m3: n(m.cubicMeters),
      ft: n(m.meters / 0.3048),
      ft2: n(m.squareMeters / 0.09290304),
      ft3: n(m.cubicMeters / 0.028316846592),
    };
  },

  splitFactors(expr: string): string[] {
    const out: string[] = [];
    let buf = '';
    let depth = 0;
    for (let i = 0; i < expr.length; i++) {
      const c = expr[i];
      if (c === '(') {
        depth++;
        buf += c;
      } else if (c === ')') {
        depth--;
        buf += c;
      } else if (depth === 0 && (c === 'x' || c === 'X' || c === '*')) {
        const t = buf.trim();
        if (t) out.push(t);
        buf = '';
      } else {
        buf += c;
      }
    }
    const t = buf.trim();
    if (t) out.push(t);
    return out.length ? out : [expr.trim() || '0'];
  },

  parseLengthToMeters(token: string): number {
    let s = token.replace(/x/gi, '*');
    // ft and inch regex: 12' 4"
    const ftIn = /(\d+(?:\.\d+)?)'\s*-?\s*(\d+(?:\.\d+)?)?"?/;
    s = s.replace(ftIn, (_, ft, inch) => {
      const f = parseFloat(ft) || 0;
      const i = parseFloat(inch) || 0;
      return (f * 0.3048 + i * 0.0254).toString();
    });
    // inches only
    s = s.replace(/(\d+(?:\.\d+)?)"/, (_, inch) => {
      return ((parseFloat(inch) || 0) * 0.0254).toString();
    });
    const parsed = parseFloat(s);
    return !isNaN(parsed) ? parsed : ArchiCalcEngine.eval(s.trim() || '0');
  },
};
