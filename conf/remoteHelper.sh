mv AmazonMonitor.zip /mnt/appdynamics/machineagent/monitors/
cd /mnt/appdynamics/machineagent/monitors/
rm -rf AmazonMonitor/
unzip AmazonMonitor.zip
rm AmazonMonitor.zip
sudo truncate -s0 /mnt/appdynamics/machineagent/logs/machine-agent.log

