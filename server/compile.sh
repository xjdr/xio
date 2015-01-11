#!/bin/bash

g++ -std=c++03 -I. *.cpp -o server -lboost_system -lboost_thread -lpthread

