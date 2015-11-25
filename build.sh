#!/bin/bash
ant -buildfile MachineSimulator.xml
ant -buildfile Scheduler.xml
ant -buildfile MaintenanceDept.xml
ant -buildfile IndustrySmartware/build/build.xml
