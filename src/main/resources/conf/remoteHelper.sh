rm -rf /mnt/appdynamics/machineagent/monitors/CloudWatchMonitor
mv CloudWatchMonitor /mnt/appdynamics/machineagent/monitors/
sudo truncate -s0 /mnt/appdynamics/machineagent/logs/machine-agent.log

