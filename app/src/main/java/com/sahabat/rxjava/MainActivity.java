package com.sahabat.rxjava;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import com.jakewharton.rxbinding3.view.RxView;
import com.sahabat.rxjava.models.Task;
import com.sahabat.rxjava.utils.DataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button button;
    private CompositeDisposable disposable = new CompositeDisposable();
    private long timeSinceLastRequest;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchView = findViewById(R.id.search_view);
        button = findViewById(R.id.button);

        timeSinceLastRequest = System.currentTimeMillis();

        throtleFirst();
    }

    public void throtleFirst(){
        RxView.clicks(button)
                .throttleFirst(5000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Unit>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onNext(Unit unit) {
                        Log.d(TAG, "onNext: time since last clicked: " + (System.currentTimeMillis() - timeSinceLastRequest));
                       someMethod();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    public void someMethod(){
        Toast.makeText(getApplicationContext(), "Appear", Toast.LENGTH_SHORT).show();
    }

    public void debounceRx(){
       Observable<String> observableQueryText = Observable
               .create(new ObservableOnSubscribe<String>() {
                   @Override
                   public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                       searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                           @Override
                           public boolean onQueryTextSubmit(String query) {
                               return false;
                           }

                           @Override
                           public boolean onQueryTextChange(String newText) {
                               if (!emitter.isDisposed()){
                                   emitter.onNext(newText);
                               }
                               return false;
                           }
                       });
                   }
               })
               .debounce(1000, TimeUnit.MILLISECONDS)
               .observeOn(Schedulers.io());

       observableQueryText.subscribe(new Observer<String>() {
           @Override
           public void onSubscribe(Disposable d) {
               disposable.add(d);
           }

           @Override
           public void onNext(String s) {
               Log.d(TAG, "onNext: time last request, " + (System.currentTimeMillis() - timeSinceLastRequest));
               Log.d(TAG, "onNext: search query, " + s);
           }

           @Override
           public void onError(Throwable e) {

           }

           @Override
           public void onComplete() {

           }
       });
    }

    public void bufferRx(){
        Observable<Task> buffer = Observable
                .fromIterable(DataSource.createTasksList())
                .subscribeOn(Schedulers.io());

        buffer
                .buffer(2)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Task>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<Task> tasks) {
                        Log.d(TAG, "onNext: result-------");
                        for(Task task: tasks){
                            Log.d(TAG, "onNext: " + task.getDescription());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        RxView.clicks(findViewById(R.id.button))
                .map(new Function<Unit, Integer>() {
                    @Override
                    public Integer apply(Unit unit) throws Exception {
                        return 1;
                    }
                })
                .buffer(4, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Integer>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onNext(List<Integer> integers) {
                        Log.d(TAG, "onNext: " + integers);
                        Log.d(TAG, "onNext: You clicked " + integers.size() + " times in 4 seconds!");

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void mapOperator(){
        Observable
                .fromIterable(DataSource.createTasksList())
                .map(new Function<Task, String>() {
                    @Override
                    public String apply(Task task) throws Exception {
                        return task.getDescription();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext: " + s);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        Function<Task, String> extractDesc = new Function<Task, String>() {
            @Override
            public String apply(Task task) throws Exception {
                Log.d(TAG, "apply: doing work on thread: " + Thread.currentThread().getName());
                return task.getDescription();
            }
        };
        Function<Task, Task> completeTaskFunction = new Function<Task, Task>() {
            @Override
            public Task apply(Task task) throws Exception {
                Log.d(TAG, "apply: doing work on thread: " + Thread.currentThread().getName());
                task.setComplete(true);
                return task;
            }
        };

        //extract task to string
        Observable<String> extractDescriptionObservable = Observable
                .fromIterable(DataSource.createTasksList())
                .subscribeOn(Schedulers.io())
                .map(extractDesc)
                .observeOn(AndroidSchedulers.mainThread());

        extractDescriptionObservable.subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }
            @Override
            public void onNext(String s) {
                Log.d(TAG, "onNext: extracted description: " + s);
            }
            @Override
            public void onError(Throwable e) {
            }
            @Override
            public void onComplete() {
            }
        });

        //set data to complete
        Observable<Task> completeTaskObservable = Observable
                .fromIterable(DataSource.createTasksList())
                .subscribeOn(Schedulers.io())
                .map(completeTaskFunction)
                .observeOn(AndroidSchedulers.mainThread());

        completeTaskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
            }
            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: is this task complete? " + task.isComplete());
            }
            @Override
            public void onError(Throwable e) {
            }
            @Override
            public void onComplete() {
            }
        });
    }

    public void takeWhile(){
        Observable<Task> taskObservable = Observable
                .fromIterable(DataSource.createTasksList())
                .takeWhile(new Predicate<Task>() {
                    @Override
                    public boolean test(Task task) throws Exception {
                        return task.isComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {

            }
            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: " + task.getDescription());
            }
            @Override
            public void onError(Throwable e) {

            }
            @Override
            public void onComplete() {

            }
        });
    }

    public void distinct(){
        Observable<Task> taskObservable = Observable
                .fromIterable(DataSource.createTasksList())
                .distinct(new Function<Task, String>() {
                    @Override
                    public String apply(Task task) throws Exception {
                        return task.getDescription();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: " + task.getDescription());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void filter(){
        Observable<Task> taskObservable = Observable
                .fromIterable(DataSource.createTasksList())
                .filter(new Predicate<Task>() {
                    @Override
                    public boolean test(Task task) throws Exception {
                        return task.isComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: " + task.getDescription());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void getDataFromViewModel(){
        MainViewModel viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        viewModel.makeQuery().observe(this, new androidx.lifecycle.Observer<ResponseBody>() {
            @Override
            public void onChanged(ResponseBody responseBody) {
                Log.d(TAG, "onChanged: this is a live data response!");
                try {
                    Log.d(TAG, "onChanged: " + responseBody.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void fromFromFrom(){
        Task[] list = new Task[5];
        list[0] = (new Task("task 1",true, 4));
        list[1] = (new Task("task 2",true, 3));
        list[2] = (new Task("task 3",false, 2));
        list[3] = (new Task("task 4",false, 4));
        list[4] = (new Task("task 5",true, 3));

        Observable<Task> taskObservable = Observable
                .fromArray(list)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: " + task.getDescription());

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        List<Task> taskList = DataSource.createTasksList();

        Observable<Task> taskListObs = Observable
                .fromIterable(taskList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        taskListObs.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: " + task.getDescription());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        // create Observable (method will not execute yet)
//        Observable<Task> callable = Observable
//                .fromCallable(new Callable<Task>() {
//                    @Override
//                    public Task call() throws Exception {
//                        return MyDatabase.getTask();
//                    })
//                })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread());
//
//// method will be executed since now something has subscribed
//        callable.subscribe(new Observer<Task>() {
//            @Override
//            public void onSubscribe(Disposable d) {
//
//            }
//
//            @Override
//            public void onNext(Task task) {
//                Log.d(TAG, "onNext: : " + task.getDescription());
//            }
//
//            @Override
//            public void onError(Throwable e) {
//
//            }
//
//            @Override
//            public void onComplete() {
//
//            }
//        });
    }

    public void intervalAndTimer() {
        Observable<Long> intervalObsservable = Observable
                .interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .takeWhile(new Predicate<Long>() {
                    @Override
                    public boolean test(Long aLong) throws Exception {
                        Log.d(TAG, "test: " + aLong + ", thread: " + Thread.currentThread().getName());
                        return aLong <= 5;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());

        intervalObsservable.subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Long aLong) {
                Log.d(TAG, "onNext: " + aLong);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        Observable<Long> timerObs = Observable
                .timer(3,TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        timerObs.subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Long aLong) {
                Log.d(TAG, "onNextTimer: " + aLong);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void justRangeOperator(){
       Observable<Integer> observable = Observable
               .range(0,3)
               .repeat(3)
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread());

       observable.subscribe(new Observer<Integer>() {
           @Override
           public void onSubscribe(Disposable d) {

           }

           @Override
           public void onNext(Integer integer) {
               Log.d(TAG, "onNext: " + integer);
           }

           @Override
           public void onError(Throwable e) {

           }

           @Override
           public void onComplete() {

           }
       });

       Observable<Task> taskObservable = Observable
               .range(0,9)
               .subscribeOn(Schedulers.io())
               .map(new Function<Integer, Task>() {
                   @Override
                   public Task apply(Integer integer) throws Exception {
                       Log.d(TAG, "apply: " + Thread.currentThread().getName());
                       return new Task("priority : " + (integer), false, integer);
                   }
               })
               .takeWhile(new Predicate<Task>() {
                   @Override
                   public boolean test(Task task) throws Exception {
                       return task.getPriority() < 9;
                   }
               })
               .observeOn(AndroidSchedulers.mainThread());

       taskObservable.subscribe(new Observer<Task>() {
           @Override
           public void onSubscribe(Disposable d) {

           }

           @Override
           public void onNext(Task task) {
               Log.d(TAG, "onNextTest: " + task.getPriority());
           }

           @Override
           public void onError(Throwable e) {

           }

           @Override
           public void onComplete() {

           }
       });
    }

    public void operatorTest(){
        final Task task =  new Task("Test Desc", true, 3);
        final List<Task> tasks = DataSource.createTasksList();

        Observable<Task> taskObservable = Observable
                .create(new ObservableOnSubscribe<Task>() {
                    @Override
                    public void subscribe(ObservableEmitter<Task> emitter) throws Exception {
                        for (Task task: DataSource.createTasksList()){
                            if (!emitter.isDisposed())
                                emitter.onNext(task);
                        }

                        if (!emitter.isDisposed())
                            emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: "+task.getDescription());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void obsTest(){
        Observable<Task> taskObservable = Observable
                .fromIterable(DataSource.createTasksList())
                .subscribeOn(Schedulers.io())
                .filter(new Predicate<Task>() {
                    @Override
                    public boolean test(Task task) throws Exception {
                        Log.d(TAG, "test: " + Thread.currentThread().getName());
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e){
                            Log.e(TAG, "test: ",e);
                        }
                        return task.isComplete();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());

        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "onSubscribe: called");
                disposable.add(d);
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: " + Thread.currentThread().getName());
                Log.d(TAG, "onNext: " + task.getDescription());
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: called");
            }
        });

        disposable.add(taskObservable.subscribe(new Consumer<Task>() {
            @Override
            public void accept(Task task) throws Exception {

            }
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }
}
