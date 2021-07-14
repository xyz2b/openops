package org.xyz.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CmdResult implements Serializable {
    private int status;
    private String stdOut;
    private String stdErrOut;
    private String error;
    private String client;
    private String uuid;
    private String startTime;
    private String endTime;
}
