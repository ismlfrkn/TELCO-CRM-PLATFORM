package com.turkcell.usage.client;

public class TariffClientDto {

    private String code;
    private Integer minutesIncluded;
    private Integer smsIncluded;
    private Integer dataMbIncluded;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getMinutesIncluded() { return minutesIncluded; }
    public void setMinutesIncluded(Integer minutesIncluded) { this.minutesIncluded = minutesIncluded; }

    public Integer getSmsIncluded() { return smsIncluded; }
    public void setSmsIncluded(Integer smsIncluded) { this.smsIncluded = smsIncluded; }

    public Integer getDataMbIncluded() { return dataMbIncluded; }
    public void setDataMbIncluded(Integer dataMbIncluded) { this.dataMbIncluded = dataMbIncluded; }
}
