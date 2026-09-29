function getBackendTarget() {
  if (process.env.BACKEND_URL) {
    return process.env.BACKEND_URL;
  }
  return 'http://localhost:8080';
}

const target = getBackendTarget();
console.log(`[proxy.conf.js] Forwarding /api -> ${target}`);

module.exports = {
  "/api": {
    target: target,
    secure: false,
    changeOrigin: true
  }
};

