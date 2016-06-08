%define __jar_repack {%nil}

# For use below
%define _prefix %{_usr}/lib/dpn
%define _confdir /etc/dpn
%define service dpn-intake

Name: dpn-intake
Version: %{ver}
Release: %{rel}%{?dist}
Source: dpn-intake.jar
Source1: dpn-intake.sh
Source2: application.yml
Summary: Chronopolis Replication Service
License: UMD
URL: https://gitlab.umiacs.umd.edu/shake/dpn-rest
Group: System Environment/Daemons
Requires: /usr/sbin/groupadd /usr/sbin/useradd
autoprov: yes
autoreq: yes
BuildArch: noarch
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The DPN Intake Service queries DPN nodes for replications to pull into
Chronopolis, and notifies the Chronopolis Ingest Service upon completion. It
also takes care of other tasks, such as synchronization and cleaning of the 
staging area.

%install

rm -rf "%{buildroot}"
%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{_prefix}/%{service}.jar"

%__install -d "%{buildroot}/var/log/dpn"
%__install -d "%{buildroot}/etc/dpn"

%__install -D -m0755 "%{SOURCE1}" "%{buildroot}/etc/init.d/%{service}"
%__install -D -m0600 "%{SOURCE2}" "%{buildroot}%{_confdir}/application.yml"


%files

%defattr(-,chronopolis,chronopolis)
# conf
%dir %{_confdir}
%config %attr(0644,-,-) %{_confdir}/application.yml
# jar
%dir %attr(0755,chronopolis,chronopolis) %{_prefix}
%{_prefix}/%{service}.jar
# init/log
%config(noreplace) /etc/init.d/%{service}
%dir %attr(0755,chronopolis,chronopolis) /var/log/dpn

%pre
/usr/sbin/groupadd -r chronopolis > /dev/null 2>&1 || :
/usr/sbin/useradd -r -g chronopolis -c "Chronopolis Service User" \
        -s /bin/bash -d /usr/lib/chronopolis/ chronopolis > /dev/null 2>&1 || :
