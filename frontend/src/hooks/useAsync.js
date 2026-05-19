import { useCallback, useEffect, useRef, useState } from 'react';

// Hook for one-shot async data loads. Tracks loading/error state and prevents
// state updates after unmount.
export function useAsync(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const mounted = useRef(true);

  const run = useCallback(async () => {
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const data = await loader();
      if (mounted.current) setState({ data, loading: false, error: null });
    } catch (error) {
      if (mounted.current) setState({ data: null, loading: false, error });
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    mounted.current = true;
    run();
    return () => { mounted.current = false; };
  }, [run]);

  return { ...state, reload: run };
}
