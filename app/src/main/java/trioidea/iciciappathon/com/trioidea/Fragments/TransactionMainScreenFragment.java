package trioidea.iciciappathon.com.trioidea.Fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import trioidea.iciciappathon.com.trioidea.Activities.TransferActivity;
import trioidea.iciciappathon.com.trioidea.DTO.TransactionDTOEncrypted;
import trioidea.iciciappathon.com.trioidea.DTO.TransactionDto;
import trioidea.iciciappathon.com.trioidea.DbHelper;
import trioidea.iciciappathon.com.trioidea.EncryptionClass;
import trioidea.iciciappathon.com.trioidea.EventNumbers;
import trioidea.iciciappathon.com.trioidea.EventResponse;
import trioidea.iciciappathon.com.trioidea.R;
import trioidea.iciciappathon.com.trioidea.RxBus;
import trioidea.iciciappathon.com.trioidea.Services.FileServerAsyncTask;

/**
 * Created by Harshal on 12-Apr-17.
 */
public class TransactionMainScreenFragment extends Fragment implements Observer {
    ProgressDialog progressDialog;
    private TextView statusText;
    byte buf[] = new byte[64];
    ServerSocket serverSocket;
    Socket client;
    TransferActivity parentActivity;
    TransactionDto transactionData;
    Subscription subscription, rxSubscription;
    String receiverName;
    long receiverId;
    RxBus rxBus;
    InputStream inputStream;
    OutputStream outputStream;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        rxBus = RxBus.getInstance();
        rxSubscription = rxBus.toObserverable().subscribe(this);
        return layoutInflater.inflate(R.layout.transaction_main_screen, viewGroup, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        initUi();
    }

    public void initUi()

    {
        parentActivity = (TransferActivity) TransactionMainScreenFragment.this.getActivity();
        parentActivity.textView = (TextView) parentActivity.findViewById(R.id.balance);
        parentActivity.textView.setText(String.valueOf(parentActivity.balance));

        getActivity().findViewById(R.id.btn_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TransferActivity) getActivity()).startHistoryFragment();
                    }
                });
            }
        });
        getActivity().findViewById(R.id.wifi_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("p2p", "discoverPeers() called--sender");
                parentActivity.getManager()
                        .discoverPeers(parentActivity.getChannel(), new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.e("p2p", "discoverPeers() Success--sender");
                                parentActivity.startSendScreenFragment();
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.e("p2p", "discoverPeers() Failure: " + reason);
                                if (reason == 2)
                                    Toast.makeText(getActivity(), "Start your wifi and try again", Toast.LENGTH_SHORT).show();
                            }

                        });
            }
        });

        (getActivity().findViewById(R.id.wifi_receive)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = new EditText(TransactionMainScreenFragment.this.getActivity());
                editText.setHint("passkey");


                progressDialog = new ProgressDialog(TransactionMainScreenFragment.this.getActivity());
                progressDialog.setMessage("Waiting for sender..");
                progressDialog.setView(editText);
                progressDialog.setProgress(0);
                progressDialog.setCancelable(false);

                ConnectivityManager connManager = (ConnectivityManager) parentActivity.getSystemService(parentActivity.getApplicationContext().CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (!mWifi.isConnected())
                    Toast.makeText(getActivity(), "Start your wifi and try again", Toast.LENGTH_SHORT).show();
                else {
                    final AlertDialog alertDialog = new AlertDialog.Builder(TransactionMainScreenFragment.this.getActivity()).create();
                    alertDialog.setMessage("Are you sure you want to Receive Money?");
                    alertDialog.setTitle("Alert");
                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectivityManager connManager = (ConnectivityManager) parentActivity.getSystemService(parentActivity.getApplicationContext().CONNECTIVITY_SERVICE);
                            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                            if (!mWifi.isConnected())
                                Toast.makeText(getActivity(), "Start your wifi and try again", Toast.LENGTH_SHORT).show();
                            if (mWifi.isConnected()) {
                                progressDialog.show();
                                Log.e("p2p", "discoverPeers() called--receiver");
                                (parentActivity).getManager()
                                        .discoverPeers(parentActivity.getChannel(), new WifiP2pManager.ActionListener() {

                                            @Override
                                            public void onSuccess() {
                                                Log.e("p2p", "discoverPeers() Success--receiver");
                                                //new FileServerAsyncTask(getActivity(),(TransferActivity)getActivity()).execute();
                                                Observable observable = Observable.fromCallable(new Callable() {
                                                    @Override
                                                    public Object call() throws Exception {
                                                        Log.e("p2p", "do in background of server");
                                                        try {
                                                            serverSocket = new ServerSocket(8888);
                                                            client = serverSocket.accept();

                                                            Log.e("p2p", "accepted socket");

                                                            outputStream = client.getOutputStream();
                                                            inputStream = client.getInputStream();
                                                            transactionData = new TransactionDto();

                                                            String timeSettings = android.provider.Settings.System.getString(getActivity().getContentResolver(), android.provider.Settings.Global.AUTO_TIME);
                                                            if (timeSettings.equals("0")) {
                                                                Settings.System.putString(getActivity().getContentResolver(), android.provider.Settings.Global.AUTO_TIME, "1");
                                                                getActivity().startActivityForResult(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS), 0);
                                                            }
                                                            double now = System.currentTimeMillis();
                                                            Log.e("Time ", "" + now);

//                                                            if (inputStream.read(buf) != 0) {
                                                            inputStream.read(buf);
                                                            String received = new String(buf).trim();
                                                            Log.e("p2p", EncryptionClass.symmetricDecrypt(received));

                                                            if (EncryptionClass.symmetricDecrypt(received).equals("passkey")) {
                                                                EventResponse eventResponse = new EventResponse(null, EventNumbers.ENTER_PASSKEY);
                                                                rxBus.send(eventResponse);
                                                            }


                                                        } catch (Exception e) {
                                                        }
                                                        return null;
                                                    }

                                                });
                                                subscription = observable.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation()).subscribe(TransactionMainScreenFragment.this);

                                            }

                                            @Override
                                            public void onFailure(int reason) {
                                                Log.e("p2p", "discoverPeers() Failure: " + reason);
                                                if (reason == 2)
                                                    Toast.makeText(getActivity(), "Start your wifi and try again", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    });
                    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.show();
                }
            }
        });
    }


    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(Object o) {
        EventResponse eventResponse = (EventResponse) o;
        switch (((EventResponse) o).getEvent()) {
            case EventNumbers.SERVER_ASYNC_EVENT:
                parentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parentActivity.textView.setText(String.valueOf(parentActivity.balance));
                        progressDialog.dismiss();

                    }
                });

                // adding data in database----------------------------------------------------------
                DbHelper db = DbHelper.getInstance(this.getActivity());
                db.insertTransaction(transactionData);
                TransactionDto[] transactionDtos = db.getAllTransaction();
                for (int i = 0; i < transactionDtos.length; i++)
                    Log.e("receiver", "length:" + transactionDtos.length + " first:" + transactionDtos[i].getAmount());
                break;
            case EventNumbers.ENTER_PASSKEY: {
                final EditText txtUrl = new EditText(parentActivity);
                txtUrl.setInputType(InputType.TYPE_NUMBER_VARIATION_NORMAL);

// Set the default text to a link of the Queen
                txtUrl.setHint("Enter Passkey");

                parentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        new AlertDialog.Builder(parentActivity)
                                .setTitle("Alert!")
                                .setMessage("Enter passkey")
                                .setView(txtUrl)
                                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Observable observable = Observable.fromCallable(new Callable() {
                                            @Override
                                            public Object call() throws Exception {
                                                Log.e("p2p", "success!!!!");

                                                try {
                                                    try {
                                                        outputStream.write((EncryptionClass.symmetricEncrypt(txtUrl.getText().toString())).getBytes());
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    Log.e("p2p", "passkey given");

                                                    buf = null;
                                                    buf = new byte[2048];
                                                    try {
                                                        inputStream.read(buf);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }

                                                    String received = new String(buf).trim();
                                                    received = EncryptionClass.symmetricDecrypt(received);
                                                    String[] receivedStrings = received.split(":");
                                                    Log.e("p2p", "data received" + received);

                                                    String amount = receivedStrings[0].trim();
                                                    parentActivity.balance = parentActivity.balance + Double.parseDouble(amount);
                                                    Log.e("p2p", "Updated balance : " + parentActivity.balance);


                                                    double now = System.currentTimeMillis();
                                                    Log.e("Time ", "" + now);

                                                    SharedPreferences sharedPreferences = parentActivity.getApplicationContext().getSharedPreferences("userData", 0);
                                                    receiverName = EncryptionClass.symmetricDecrypt(sharedPreferences.getString("name", "User"));
                                                    receiverId = Long.parseLong(EncryptionClass.symmetricDecrypt(sharedPreferences.getString("account", "0000")));
                                                    transactionData.setAmount(Double.parseDouble(amount));
                                                    transactionData.setReceiverId(receiverId);
                                                    transactionData.setReceiverName(receiverName);
                                                    transactionData.setSenderID(Long.parseLong(receivedStrings[1]));
                                                    transactionData.setSenderName(receivedStrings[2]);
                                                    transactionData.setTime(String.valueOf(now));
                                                    transactionData.setSyncFlag(false);
                                                    transactionData.setBalance(parentActivity.balance);
                                                    // }

                                                    buf = EncryptionClass.symmetricEncrypt("s" + ":" + receiverId + ":" + receiverName + ":" + now).getBytes();
                                                    Log.e("receiver", "buf" + buf);
                                                    try {
                                                        outputStream.write(buf);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    Log.e("p2p", "data sent back");
                                                    buf = null;
                                                    buf = new byte[2048];
                                                    try {
                                                        if (inputStream.read(buf) != 0) {
                                                            received = new String(buf).trim();
                                                            //received = EncryptionClass.symmetricDecrypt(received);
                                                            if (received == "s") {
                                                                Log.e("p2p", "Transaction Complete for Server" + received);
                                                            }
                                                        }
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }

                                                } finally {
                                                    if (serverSocket != null) {
//                if (serverSocket.isBound() || !serverSocket.isClosed())
                                                        {
                                                            try {

                                                                Log.e("p2p", "Closing ServerSocket");
                                                                serverSocket.close();
                                                                client.close();//----
                                                                parentActivity.disconnect();
                                                                parentActivity.mWifiP2pManager.requestGroupInfo(parentActivity.mChannel, new WifiP2pManager.GroupInfoListener() {
                                                                    @Override
                                                                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                                                                        parentActivity.deletePersistentGroup(group);
                                                                        Log.e("p2p", " group removed");
                                                                    }
                                                                });


//                        activity.mIntentFilter=null;
//                        activity.mWifiP2pManager.removeGroup(activity.mChannel, new WifiP2pManager.ActionListener() {
//                            @Override
//                            public void onSuccess() {
//                                Log.e("p2p", "Closing wifi connection");
//                            }
//
//                            @Override
//                            public void onFailure(int reason) {
//                                Log.e("p2p", "Failed to close wifi connection.. reason: "+reason);
//                            }
//                        });
                                                            } catch (IOException e) {
                                                                //catch logic
                                                            }
                                                        }
                                                    }
                                                    parentActivity.mobileNames.clear();
                                                    parentActivity.mobiles.clear();
                                                }
                                                EventResponse eventResponse = new EventResponse(transactionData, EventNumbers.SERVER_ASYNC_EVENT);

                                                return eventResponse;

                                            }
                                        });
                                        subscription = observable.observeOn(Schedulers.io()).subscribeOn(Schedulers.io()).subscribe(TransactionMainScreenFragment.this);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        try {
                                            outputStream.write((EncryptionClass.symmetricEncrypt("failed")).getBytes());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }finally {
                                            if (serverSocket != null) {
//                if (serverSocket.isBound() || !serverSocket.isClosed())
                                                {
                                                    try {

                                                        Log.e("p2p", "Closing ServerSocket");
                                                        serverSocket.close();
                                                        client.close();//----
                                                        parentActivity.disconnect();
                                                        parentActivity.mWifiP2pManager.requestGroupInfo(parentActivity.mChannel, new WifiP2pManager.GroupInfoListener() {
                                                            @Override
                                                            public void onGroupInfoAvailable(WifiP2pGroup group) {
                                                                parentActivity.deletePersistentGroup(group);
                                                                Log.e("p2p", " group removed");
                                                            }
                                                        });


//                        activity.mIntentFilter=null;
//                        activity.mWifiP2pManager.removeGroup(activity.mChannel, new WifiP2pManager.ActionListener() {
//                            @Override
//                            public void onSuccess() {
//                                Log.e("p2p", "Closing wifi connection");
//                            }
//
//                            @Override
//                            public void onFailure(int reason) {
//                                Log.e("p2p", "Failed to close wifi connection.. reason: "+reason);
//                            }
//                        });
                                                    } catch (IOException e) {
                                                        //catch logic
                                                    }
                                                }
                                            }
                                            parentActivity.mobileNames.clear();
                                            parentActivity.mobiles.clear();
                                        }
                                    }
                                })
                                .show();

                    }
                });


            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (subscription != null)
            subscription.unsubscribe();
        if (rxSubscription != null)
            rxSubscription.unsubscribe();
    }
}
