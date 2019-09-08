dns-gigapaxos
=============

Resolve a service name by asking a reconfigurator.

### Usage

```bash
$ sudo systemctl stop systemd-resolved
$ sudo java -jar -Dport=53 -Dreconfigurator=<host:port> dns-gigapaxos-all.jar
```

