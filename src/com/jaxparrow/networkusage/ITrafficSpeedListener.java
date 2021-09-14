package com.jaxparrow.networkusage;

public interface ITrafficSpeedListener {

    void onTrafficSpeedMeasured(double upStream, double downStream);
}