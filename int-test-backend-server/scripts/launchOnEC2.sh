#!/bin/bash
/home/admin/app/int-test-backend-server-0.13.9-SNAPSHOT/bin/int-test-backend-server $(curl http://169.254.169.254/latest/meta-data/local-ipv4) 8080 $(curl http://169.254.169.254/latest/meta-data/local-ipv4)
