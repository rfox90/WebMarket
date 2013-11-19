package com.survivorserver.Dasfaust.WebMarket.mojang.profiles;

public interface ProfileRepository {
    public Profile[] findProfilesByCriteria(ProfileCriteria... criteria);
}
