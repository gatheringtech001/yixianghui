module.exports = {
  apps: [
    {
      name: "yixianghui-knowledge",
      cwd: "/opt/yixianghui-knowledge",
      script: "server.mjs",
      node_args: "--env-file=/etc/yixianghui-knowledge.env",
      instances: 1,
      autorestart: true,
      max_memory_restart: "512M",
      time: true,
      env: { NODE_ENV: "production" },
    },
  ],
};
