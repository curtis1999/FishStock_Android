package com.example.fishstock;

import java.util.Random;

public class Coordinate {
  public int file;
  public int rank;

  public Coordinate(int file, int rank) {
    this.rank = rank;
    this.file = file;
  }
  public void setCoord(int file, int rank) {
    this.file=file;
    this.rank=rank;
  }
  public Coordinate copyCoordinate() {
    return new Coordinate(this.file, this.rank);
  }
  public static boolean compareCoords(Coordinate c1, Coordinate c2) {
    return ((c1.file==c2.file) && (c1.rank == c2.rank));
  }
  public static Coordinate generateRandomCoordinate() {
    int rFile = (int) new Random().nextInt(7);
    int rRank = (int) new Random().nextInt(7);
    return new Coordinate(rFile, rRank);
  }
}

