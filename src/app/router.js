export function createRouter() {
  const routes = new Map();

  return {
    register(method, pathname, handler) {
      routes.set(`${method.toUpperCase()} ${pathname}`, handler);
    },
    match(method, pathname) {
      return routes.get(`${method.toUpperCase()} ${pathname}`) ?? null;
    }
  };
}
