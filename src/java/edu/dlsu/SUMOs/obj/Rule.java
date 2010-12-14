package edu.dlsu.SUMOs.obj;

public class Rule {

  private String orig = null;
  private String manner = null;
  private String experiencer = null;
  private String agent = null;
  private String instance = null;
  private String rule = null;
  private String patient = null;
  
  public String getPatient() {
    return patient;
  }

  public void setPatient(String patient) {
    this.patient = patient;
  }

  public String getOrig() {
    return orig;
  }

  public void setOrig(String orig) {
    this.orig = orig;
  }
  
  public String getRule() {
    return rule;
  }

  public void setRule(String rule) {
    this.rule = rule;
  }
  
  private int attributeCnt;
  
  public Rule() {
    attributeCnt = 0;
  }
  
  public void addAttributeCnt() {
    attributeCnt++;
  }
  
  public int getAttributeCnt() {
    return attributeCnt;
  }

  public void setAttributeCnt(int attributeCnt) {
    this.attributeCnt = attributeCnt;
  }

  public String getManner() {
    return manner;
  }
  public void setManner(String manner) {
    this.manner = manner;
  }
  public String getExperiencer() {
    return experiencer;
  }
  public void setExperiencer(String experiencer) {
    this.experiencer = experiencer;
  }
  public String getAgent() {
    return agent;
  }
  public void setAgent(String agent) {
    this.agent = agent;
  }
  public String getInstance() {
    return instance;
  }
  public void setInstance(String instance) {
    this.instance = instance;
  }
}
