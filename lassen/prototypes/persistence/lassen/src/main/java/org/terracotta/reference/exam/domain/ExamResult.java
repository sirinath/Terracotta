package org.terracotta.reference.exam.domain;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "RESUlTS")
public class ExamResult {
  
  @Id @GeneratedValue
  @Column(name="RESULT_ID")
  private Long id;

  /* join column */
  @Column(name="TEST_ID")
  private Long testId;
  
  @Column(name="START_TIME")
  private Timestamp startTime;
  
  @Column(name="END_TIME")
  private Timestamp endTime;
  
  @Column(name="SCORE")
  private int score;
  
  @Column(name="PASS")
  private boolean pass;

  public ExamResult() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTestId() {
    return testId;
  }

  public void setTestId(Long testId) {
    this.testId = testId;
  }

  public Timestamp getStartTime() {
    return startTime;
  }

  public void setStartTime(Timestamp startTime) {
    this.startTime = startTime;
  }

  public Timestamp getEndTime() {
    return endTime;
  }

  public void setEndTime(Timestamp endTime) {
    this.endTime = endTime;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public boolean isPass() {
    return pass;
  }

  public void setPass(boolean pass) {
    this.pass = pass;
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    } else if(!(obj instanceof User)) {
      return false;
    } else {
      return ((ExamResult)obj).getId().equals(id);
    }
  }
}
