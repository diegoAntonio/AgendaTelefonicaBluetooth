// SeletorDispositivo.java
// Activity para a escolha de um dispositivo de ligação.
package src.agendaTelefonica;

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
	// Comprimento de um endereço MAC em caracteres
	private static final int MAC_ADDRESS_LENGTH = 17;

	// A chave para armazenar o endereço MAC do dispositivo selecionado como um Intent extra
	public static final String DEVICE_ADDRESS = "device_address";

	private BluetoothAdapter bluetoothAdapter; // O Adaptador Bluetooth
	private ArrayAdapter<String> foundDevicesAdapter; // Dados ListView
	private ListView newDevicesListView; // ListView que mostra os dispositivos

	// Chamado quando esta Activity  é criada pela primeira vez
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		// Mostra uma barra de progresso enquanto cargas de atividade
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Define o layout da Atividade
		setContentView(R.layout.device_chooser_layout);

		// Define o código de resultado para voltar à Activity anterior
		// Se o usuário toca o botão "Cancel"
		setResult(Activity.RESULT_CANCELED);

		// Cria botão para iniciar a descoberta
		Button scanButton = (Button) findViewById(R.id.scanButton);
		scanButton.setOnClickListener(
				new OnClickListener() 
				{
					// Chamado quando o scanButton é clicado
					public void onClick(View v) 
					{
						startDiscovery(); // Começar a procurar dispositivos
					} // Fim do metodo onClick
				} // Fim de OnClickListener
				); // Fim de call to setOnClickListener

		// Inicializar o adaptador de lista para lista de dispositivos encontrados
		foundDevicesAdapter = 
				new ArrayAdapter<String>(this, R.layout.device_layout);

		// Inicializar o ListView que irá exibir os dispositivos recém-descobertos
		newDevicesListView = getListView();
		newDevicesListView.setAdapter(foundDevicesAdapter);
		newDevicesListView.setOnItemClickListener(
				deviceListItemClickListener);

		// Ouvir Intents de transmissão alertando que um dispositivo Bluetooth 
		// Foi encontrado nas proximidades
		IntentFilter filter = 
				new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(deviceChooserReceiver, filter);

		// Ouvir Intents de transmissão alertando que a busca por
		// Dispositivos próximos foi concluída
		filter = 
				new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(deviceChooserReceiver, filter);

		// Obtém o BluetoothAdapter local
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Obtém um conjunto de todos os dispositivos que já estão conectados
		Set<BluetoothDevice> pairedDevices = 
				bluetoothAdapter.getBondedDevices();

		// Adiciona o nome de cada dispositivo conectado ao nosso ListView
		for (BluetoothDevice device : pairedDevices) 
		{
			foundDevicesAdapter.add(device.getName() + "\n" + 
					device.getAddress());
		} // Fim de for
	} // Fim do metodo onCreate

	// Chamada antes dessa Activity é destruída
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

	// Começar a descoberta
	private void startDiscovery() 
	{
		// Verifica se o Bluetooth ainda está habilitado
		if (!bluetoothAdapter.isEnabled()) 
		{
			Toast.makeText(this, R.string.no_bluetooth, Toast.LENGTH_LONG);
			return;
		} // Fim do if

		// Fim de descoberta existente, se necessário
		if (bluetoothAdapter.isDiscovering()) 
		{
			bluetoothAdapter.cancelDiscovery();
		} // Fim do if

		// Mostrar a barra de progresso
		setProgressBarIndeterminateVisibility(true);

		// Começar a procurar por outros dispositivos
		bluetoothAdapter.startDiscovery();
	} // Fim do metodo startDiscovery

	// Escuta para os eventos gerados quando o usuário clica item de ListView
	private OnItemClickListener deviceListItemClickListener = 
			new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> parent, View view,  
				int position, long id) 
		{
			// Cancelar a descoberta antes de tentar conectar
			bluetoothAdapter.cancelDiscovery();

			// Pega o endereço MAC do dispositivo selecionado
			String info = ((TextView) view).getText().toString();
			String address = info.substring(info.length() - 
					MAC_ADDRESS_LENGTH);

			// Cria a intenção de retornar à chamada Activity
			Intent intent = new Intent();

			// Incluir o endereço MAC do dispositivo na Intent de retorno
			intent.putExtra(DEVICE_ADDRESS, address);

			// Define a nossa Intent, como o valor de retorno de sucesso e acabamento
			setResult(Activity.RESULT_OK, intent);
			finish();
		} // Fim do metodo onItemClick
	}; // Fim de OnItemClickListener

	// Escuta para transmissão de Intents anunciando quando uma
	// descoberta finaliza e quando novos dispositivos são detectados
	private final BroadcastReceiver deviceChooserReceiver = 
			new BroadcastReceiver() 
	{
		// Chamada quando uma transmissão é recebido
		public void onReceive(Context context, Intent intent) 
		{
			// Obtém a ação do Intenção chamando
			String action = intent.getAction(); 

			// Um novo dispositivo foi detectado
			if (BluetoothDevice.ACTION_FOUND.equals(action)) 
			{
				// Obtém o BluetoothDevice da Intent de transmissão
				BluetoothDevice device = 
						intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				// Se o dispositivo não estiver conectado, adicionar seu nome
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

				// Define o título da Activity
				setTitle(getResources().getString(R.string.choose_device)); 

				// Se não houvesse dispositivos no intervalo, exibir uma mensagem 
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