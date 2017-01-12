# Monitoring Agent [![Build Status](https://travis-ci.org/dishmael/java-monitoring-agent.svg?branch=master)](https://travis-ci.org/dishmael/java-monitoring-agent)
This is a java-based agent I wrote for a customer who had a need to monitor legacy equipment.  The agent uses [Quartz Scheduling](http://www.quartz-scheduler.org) for scheduled tasks and [Apache Commons Procrun](https://commons.apache.org/proper/commons-daemon/procrun.html) utility so the agent will run as a Microsoft Windows service.  The agent settings can be controlled using the config.properties file located under /src/main/resources.

## Instructions
You can find the following instructions on [Stack Overflow](http://stackoverflow.com/questions/68113/how-to-create-a-windows-service-from-java-app), but I will put them here for posterity.

With Apache Commons Daemon you can now have a custom executable name and icon! You can also get a custom Windows tray monitor with your own name and icon!

I now have my service running with my own name and icon (prunsrv.exe), and the system tray monitor (prunmgr.exe) also has my own custom name and icon!

1. Download the [Apache Commons Daemon binaries](http://www.apache.org/dist/commons/daemon/binaries/windows/) (you will need prunsrv.exe and prunmgr.exe).
2. Rename them to be MyServiceName.exe and MyServiceNamew.exe respectively.
3. Download [WinRun4J](http://winrun4j.sourceforge.net/) and use the RCEDIT.exe program that comes with it to modify the Apache executable to embed your own custom icon like this:
```
> RCEDIT.exe /I MyServiceName.exe customIcon.ico
> RCEDIT.exe /I MyServiceNamew.exe customTrayIcon.ico
```
4. Now install your Windows service like this (see [documentation](http://commons.apache.org/daemon/procrun.html) for more details and options):
```
> MyServiceName.exe //IS//MyServiceName \
  --Install="C:\path-to\MyServiceName.exe" \
  --Jvm=auto --Startup=auto --StartMode=jvm \
  --Classpath="C:\path-to\MyJarWithClassWithMainMethod.jar" \
  --StartClass=com.mydomain.MyClassWithMainMethod
```
5. Now you have a Windows service of your Jar that will run with your own icon and name! You can also launch the monitor file and it will run in the system tray with your own icon and name.
