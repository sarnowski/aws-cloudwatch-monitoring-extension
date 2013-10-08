mv CloudWatchMonitor.zip /mnt/appdynamics/machineagent/monitors/
cd /mnt/appdynamics/machineagent/monitors/
rm -rf CloudWatchMonitor/
unzip CloudWatchMonitor.zip
rm CloudWatchMonitor.zip
sudo truncate -s0 /mnt/appdynamics/machineagent/logs/machine-agent.log

