#!/bin/bash

curl  -i -X POST -H "X-Correlation-Id: bob"  -H "Content-Type: application/json;type=validation;version=1.0.0" -d @data.json localhost:8080
