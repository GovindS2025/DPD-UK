package com.dpd.uk.common.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    
    @NotBlank
    private String line1;
    
    private String line2;
    private String line3;
    
    @NotBlank
    private String city;
    
    @NotBlank
    private String postcode;
    
    @NotBlank
    private String country;
    
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    private String instructions;
    private Boolean isResidential;
    private Boolean isBusiness;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
}
