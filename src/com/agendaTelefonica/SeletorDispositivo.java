// SeletorDispositivo.java
// Activity para a escolha de um dispositivo de liga��o.
package com.agendaTelefonica;

import java.util.Set;

import com.agendaTelefonica.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

@SuppressLint("ShowToast")
public class SeletorDispositivo extends ListActivity 
{
	// Comprimento de um endere�o MAC em caracteres
	private static final int MAC_ADDRESS_LENGTH = 17;

	// A chave para armazenar o endere�o MAC do dispositivo selecionado como um Intent extra
	public static final String DEVICE_ADDRESS = "device_address";

	private BluetoothAdapter bluetoothAdapter; // O Adaptador Bluetooth
	private ArrayAdapter<String> foundDevicesAdapter; // Dados ListView
	private ListView newDevicesListView; // ListView que mostra os dispositivos

	// Chamado quando esta Activity  � criada pela primeira vez
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		// Mostra uma barra de progresso enquanto cargas de atividade
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Define o layout da Atividade
		setContentView(R.layout.device_chooser_layout);

		// Define o c�digo de resultado para voltar � Activity anterior
		// Se o usu�rio toca o bot�o "Cancel"
		setResult(Activity.RESULT_CANCELED);

		// Cria bot�o para iniciar a descoberta
		Button scanButton = (Button) findViewById(R.id.scanButton);
		scanButton.setOnClickListener(
				new OnClickListener() 
				{
					// Chamado quando o scanButton � clicado
					public void onClick(View v) 
					{
						startDiscovery(); // Come�ar a procurar dispositivos
					} // Fim do metodo onClick
				} // Fim de OnClickListener
				); // Fim de call to setOnClickListener

		// Inicializar o adaptador de lista para lista de dispositivos encontrados
		foundDevicesAdapter = 
				new ArrayAdapter<String>(this, R.layout.device_layout);

		// Inicializar o ListView que ir� exibir os dispositivos rec�m-descobertos
		newDevicesListView = getListView();
		newDevicesListView.setAdapter(foundDevicesAdapter);
		newDevicesListView.setOnItemClickListener(
				deviceListItemClickListener);

		// Ouvir Intents de transmiss�o alertando que um dispositivo Bluetooth 
		// Foi encontrado nas proximidades
		IntentFilter filter = 
				new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(deviceChooserReceiver, filter);

		// Ouvir Intents de transmiss�o alertando que a busca por
		// Dispositivos pr�ximos foi conclu�da
		filter = 
				new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(deviceChooserReceiver, filter);

		// Obt�m o BluetoothAdapter local
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Obt�m um conjunto de todos os dispositivos que j� est�o conectados
		Set<BluetoothDevice> pairedDevices = 
				bluetoothAdapter.getBondedDevices();

		// Adiciona o nome de cada dispositivo conectado ao nosso ListView
		for (BluetoothDevice device : pairedDevices) 
		{
			foundDevicesAdapter.add(device.getName() + "\n" + 
					device.getAddress());
		} // Fim de for
	} // Fim do metodo onCreate

	// Chamada antes dessa Activity � destru�da
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();

		// Fim de Bluetooth discovery
		if (bluetoothAdapter != null) 
		{
			bluetoothAdapter.cancelDiscovery();
		} // Fim do if

		// Cancelar o registro do deviceChooserReceiver BroadcastReceiver
		unregisterReceiver(deviceChooserReceiver);
	} // Fim do metodo onDestroy

	// Come�ar a descoberta
	private void startDiscovery() 
	{
		// Verifica se o Bluetooth ainda est� habilitado
		if (!bluetoothAdapter.isEnabled()) 
		{
			Toast.makeText(this, R.string.no_bluetooth, Toast.LENGTH_LONG);
			return;
		} // Fim do if

		// Fim de descoberta existente, se necess�rio
		if (bluetoothAdapter.isDiscovering()) 
		{
			bluetoothAdapter.cancelDiscovery();
		} // Fim do if

		// Mostrar a barra de progresso
		setProgressBarIndeterminateVisibility(true);

		// Come�ar a procurar por outros dispositivos
		bluetoothAdapter.startDiscovery();
	} // Fim do metodo startDiscovery

	// Escuta para os eventos gerados quando o usu�rio clica item de ListView
	private OnItemClickListener deviceListItemClickListener = 
			new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> parent, View view,  
				int position, long id) 
		{
			// Cancelar a descoberta antes de tentar conectar
			bluetoothAdapter.cancelDiscovery();

			// Pega o endere�o MAC do dispositivo selecionado
			String info = ((TextView) view).getText().toString();
			String address = info.substring(info.length() - 
					MAC_ADDRESS_LENGTH);

			// Cria a inten��o de retornar � chamada Activity
			Intent intent = new Intent();

			// Incluir o endere�o MAC do dispositivo na Intent de retorno
			intent.putExtra(DEVICE_ADDRESS, address);

			// Define a nossa Intent, como o valor de retorno de sucesso e acabamento
			setResult(Activity.RESULT_OK, intent);
			finish();
		} // Fim do metodo onItemClick
	}; // Fim de OnItemClickListener

	// Escuta para transmiss�o de Intents anunciando quando uma
	// descoberta finaliza e quando novos dispositivos s�o detectados
	private final BroadcastReceiver deviceChooserReceiver = 
			new BroadcastReceiver() 
	{
		// Chamada quando uma transmiss�o � recebido
		public void onReceive(Context context, Intent intent) 
		{
			// Obt�m a a��o do Inten��o chamando
			String action = intent.getAction(); 

			// Um novo dispositivo foi detectado
			if (BluetoothDevice.ACTION_FOUND.equals(action)) 
			{
				// Obt�m o BluetoothDevice da Intent de transmiss�o
				BluetoothDevice device = 
						intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				// Se o dispositivo n�o estiver conectado, adicionar seu nome
				// Para o ListView
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) 
				{
					foundDevicesAdapter.add(device.getName() + "\n" + 
							device.getAddress());
				} // Fim do if
			} // Fim do if
			// A busca por novos dispositivos terminou 
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(
					action)) 
			{
				// Ocultar a barra de progresso 
				setProgressBarIndeterminateVisibility(false);

				// Define o t�tulo da Activity
				setTitle(getResources().getString(R.string.choose_device)); 

				// Se n�o houvesse dispositivos no intervalo, exibir uma mensagem 
				if (foundDevicesAdapter.getCount() == 0) 
				{
					// Desativar o item de lista
					newDevicesListView.setOnItemClickListener(null);
					foundDevicesAdapter.add(getResources().getString(
							R.string.no_devices));
				} // Fim do if
			} // Fim do else if
		} // Fim do metodo onReceive
	}; // Fim de BroadcastReceiver
} // Fim da classe DeviceChooser