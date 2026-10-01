((root) => {
  function initial(config, parameters, saved) {
    const known = new Map(parameters.map(param => [param.id, param]));
    return config.ids.map((id, i) => {
      const candidate = Array.isArray(saved) && saved.length === config.ids.length ? saved[i] : known.get(id)?.default_value ?? config.defaults[i];
      return Number.isInteger(candidate) ? Math.max(config.min[i], Math.min(config.max[i], candidate)) : config.defaults[i];
    });
  }
  function validate(config, values) {
    if (values.length !== config.ids.length) throw Object.assign(new Error("parameter count"), { index: -1 });
    for (let i = 0; i < values.length; i++) {
      if (!Number.isInteger(values[i]) || values[i] < config.min[i] || values[i] > config.max[i])
        throw Object.assign(new Error("parameter range"), { index: i });
    }
    return values;
  }
  root.BATTLE_RULES = { initial, validate };
})(globalThis);
