/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.User;

public class ChoresRepository {

  private final AppDatabase appDatabase;

  public ChoresRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface ChoresDataListener {

    void actionFinished(ChoresData data);
  }

  public static class ChoresData {

    private final List<ChoreEntry> choreEntries;
    private final List<Chore> chores;
    private final List<User> users;

    public ChoresData(
        List<ChoreEntry> choreEntries,
        List<Chore> chores,
        List<User> users
    ) {
      this.choreEntries = choreEntries;
      this.chores = chores;
      this.users = users;
    }

    public List<ChoreEntry> getChoreEntries() {
      return choreEntries;
    }

    public List<Chore> getChores() {
      return chores;
    }

    public List<User> getUsers() {
      return users;
    }
  }

  public void loadFromDatabase(ChoresDataListener onSuccess, Consumer<Throwable> onError) {
    Single
        .zip(
            appDatabase.choreEntryDao().getChoreEntries(),
            appDatabase.choreDao().getChores(),
            appDatabase.userDao().getUsers(),
            ChoresData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::actionFinished)
        .doOnError(onError)
        .onErrorComplete()
        .subscribe();
  }
}
