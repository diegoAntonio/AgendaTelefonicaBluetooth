// AgendaTelefonica.java
// Main activity for the Address Book app.
package com.agendaTelefonica;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.agendaTelefonica.R;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class AgendaTelefonica extends ListActivity 
{
	private static String TAG = AgendaTelefonica.class.getName();

	// Unique UUID app gerado pelo http://www.guidgenerator.com/ 
	public static final UUID MY_UUID = 
			UUID.fromString("6acc0a73-afc3-4483-a3a8-94be2c0dfc52");

	// Nome do serviço para descoberta de serviços 
	private static final String NAME = "AgendaTelefonicaBluetooth";

	// Constantes passados ​​para startActivityForResult 
	private static final int ENABLE_BLUETOOTH = 1; 
	private static final int REQUEST_DISCOVERABILITY = 2; 

	// BluetoothAdapter fornece acesso a recursos Bluetooth 
	private BluetoothAdapter bluetoothAdapter = null; 
	private boolean userAllowedBluetooth = true;
	private Handler handler; // Para a exibição de Toasts de tópicos não-GUI 

	public static final String ROW_ID = "row_id"; // Chave extra Intenção 
	private ListView contactListView; // ListView do ListActivity
	private CursorAdapter contactAdapter; // Adaptador para ListView

	// Chamado quando a atividade é criada pela primeira vez 
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState); // Chama de super do onCreate 
		contactListView = getListView(); // Obtém o ListView embutido 
		contactListView.setOnItemClickListener(viewContactListener);      

		// Mapear o nome de cada contato a um TextView no layout ListView 
		String[] from = new String[] { "name" };
		int[] to = new int[] { R.id.contactTextView };
		contactAdapter = new SimpleCursorAdapter(
				AgendaTelefonica.this, R.layout.contact_list_item, null, from, to);
		setListAdapter(contactAdapter); // Definir adaptador de ContactView

		// Obtém o adaptador Bluetooth padrão 
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		handler = new Handler(); // Para a exibição de Toasts no thread de GUI
	} // Fim do metodo onCreate 

	// Chamado quando esta atividade retorna do fundo
	@Override
	protected void onResume() 
	{
		super.onResume(); // Chama o método de super-onResume

		// Solicitar que o Bluetooth esteja ativado, se não é já
		if (!bluetoothAdapter.isEnabled() && userAllowedBluetooth) 
		{
			// Criar e começar a intenção de pedir usuário para ativar o Bluetooth
			Intent enableBluetoothIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetoothIntent, 
					ENABLE_BLUETOOTH);
		} // Fim do if

		// Cria novo GetContactsTask e executá-lo
		new GetContactsTask().execute((Object[]) null);
	} // Fim do metodo onResume

	// Quando esta atividade é interrompida, desativar Cursor para ListView
	@Override
	protected void onStop() 
	{
		Cursor cursor = contactAdapter.getCursor(); // Obtém Cursor atual

		if (cursor != null) 
			cursor.deactivate(); // Desativá-lo

		contactAdapter.changeCursor(null); // Adaptado agora não tem Cursor
		super.onStop();
	} // Fim do metodo onStop

	// Realiza consulta de banco de dados fora do thread de GUI
	private class GetContactsTask extends AsyncTask<Object, Object, Cursor> 
	{
		ConexaoBancoDados databaseConnector = 
				new ConexaoBancoDados(AgendaTelefonica.this);

		// Executar o acesso ao banco
		@Override
		protected Cursor doInBackground(Object... params)
		{
			databaseConnector.open();

			// Obtém um cursor que contém contatos de chamadas
			return databaseConnector.getAllContacts(); 
		} // Fim do metodo doInBackground

		// Usar o cursor retornado do método doInBackground
		@Override
		protected void onPostExecute(Cursor result)
		{
			contactAdapter.changeCursor(result); // Define o cursor do adaptador
			databaseConnector.close();
		} // Fim do metodo onPostExecute
	} // Fim da classe GetContactsTask

	// Cria o menu da Atividade de um arquivo XML recurso de menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.agendatelefonica_menu, menu);
		return true;
	} // Fim do metodo onCreateOptionsMenu

	// Controle de escolha de menu de opções
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
		case R.id.addContactItem: 
			// Cria uma nova intenção de lançar o AdicionarEditarContato Activity
			Intent addNewContact = 
			new Intent(AgendaTelefonica.this, AdicionarEditarContato.class);
			startActivity(addNewContact); // Inicia AdicionarEditarContato Activity
			break;
		case R.id.receiveContactItem:
			if (bluetoothAdapter.isEnabled())
			{
				// Lançamento Intenção para solicitar descoberta por 120 segundos
				Intent requestDiscoverabilityIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				startActivityForResult(requestDiscoverabilityIntent, 
						REQUEST_DISCOVERABILITY);
			} // Fim de if
			else // Usuário não permitiu adaptador Bluetooth esteja ativado
			{
				Toast.makeText(this, 
						R.string.no_bluetooth,
						Toast.LENGTH_LONG).show();
			} // Fim de else
			break;
		} // Fim de switch

		return super.onOptionsItemSelected(item); // Chama o metodo de super
	} // Fim do metodo onOptionsItemSelected

	// Ouvinte evento que responde ao utilizador
	// Tocar no nome do contato no ListView
	OnItemClickListener viewContactListener = new OnItemClickListener() 
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int id,
				long position) 
		{
			// Cria a intenção de lançar a ViewContact Activity
			Intent viewContact = 
					new Intent(AgendaTelefonica.this, VisualizacaoContato.class);

			// Passa o contato selecionado linha ID como um extra com a posicao
			viewContact.putExtra(ROW_ID, position);
			startActivity(viewContact); //Inicia a ViewContact Activity
		} // Fim do metodo onItemClick
	}; // Fim de viewContactListener

	// Chamada com resultado da startActivityForResult
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) // Processa resultado baseado na requestCode
		{
		case ENABLE_BLUETOOTH: // Tentou ativar Bluetooth
			if (resultCode == RESULT_OK) // Bluetooth foi ativado
			{
				Toast.makeText(this, 
						R.string.bluetooth_enabled,
						Toast.LENGTH_LONG).show();               
			} // Fim de if 
			else // Bluetooth não foi ativado
			{
				userAllowedBluetooth = false;
				Toast.makeText(this, R.string.no_bluetooth,
						Toast.LENGTH_LONG).show();               
			} // Fim de else
			break;
			// Tentativa de tornar o dispositivo detectável
		case REQUEST_DISCOVERABILITY: 
			if (resultCode != RESULT_CANCELED) // Usuario deu permissao
			{
				listenForContact(); //Inicia listening de uma conexao
			} // Fim de if
			else // Usuario nao permitiu descoberta
			{
				Toast.makeText(this, 
						R.string.no_discoverability,
						Toast.LENGTH_LONG).show();               
			} // Fim de else
			break;
		} // Fim de switch
	} // Fim do metodo onActivityResult

	// Inicia listening de um contacto enviados a partir de outro dispositivo
	private void listenForContact()
	{
		// Inicia tarefa de fundo que esperar por conexao
		// E receber um contato
		ReceiveContactTask task = new ReceiveContactTask();
		task.execute((Object[]) null);
	} // Fim do metodo listenForContact

	// Thread que escuta as solicitações de conexão de entrada
	private class ReceiveContactTask 
	extends AsyncTask<Object, Object, Object> 
	{
		private BluetoothServerSocket serverSocket; // Espera por conexão
		private BluetoothSocket socket; // Usado para processar conexão

		// Aguardar ligação, receber o contato e atualização da lista de contatos
		@Override
		protected Object doInBackground(Object... params)
		{
			try
			{
				// Obtem BluetoothServerSocket de bluetoothAdapter
				serverSocket = 
						bluetoothAdapter.listenUsingRfcommWithServiceRecord(
								NAME, MY_UUID);

				displayToastViaHandler(AgendaTelefonica.this, handler, 
						R.string.waiting_for_contact);

				// Espera por conexão
				BluetoothSocket socket = serverSocket.accept(); 

				// Obtém InputStream para receber contato
				InputStream inputStream = socket.getInputStream();

				// Cria um array de bytes para armazenar informações de contato de entrada
				byte[] buffer = new byte[1024];
				int bytes; // Número de bytes lidos

				// Lê a partir do InputStream e armazenar dados em buffer
				bytes = inputStream.read(buffer);

				if (bytes != -1) // Um contato foi recebido
				{
					ConexaoBancoDados databaseConnector = null;

					// Converter ReadMessage para JSONObject
					try
					{
						// Cria JSONObject de bytes de leitura
						JSONObject contact = 
								new JSONObject(new String(buffer, 0, buffer.length));

						// Cria novo DatabaseConnector
						databaseConnector = 
								new ConexaoBancoDados(getBaseContext());

						// Abre o banco de dados e adicionar o contato à base de dados
						databaseConnector.open(); // Conecta ao banco de dados

						databaseConnector.insertContact( // Adiciona o contato
								contact.getString("name"), 
								contact.getString("email"), 
								contact.getString("phone"), 
								contact.getString("street"), 
								contact.getString("city"));

						// Atualiza a lista de contatos
						new GetContactsTask().execute((Object[]) null);
						displayToastViaHandler(AgendaTelefonica.this, handler, 
								R.string.contact_received);
					} // Fim de try
					catch (JSONException e) // Problema com o formato JSON
					{        
						displayToastViaHandler(AgendaTelefonica.this, handler, 
								R.string.contact_not_received);
						Log.e(TAG, e.toString());
					} // Fim de catch
					finally // Assegura que a conexão com o banco está fechado
					{
						if (databaseConnector != null)
							databaseConnector.close(); // Fecha a conexao
					} // Fim de finally
				} // Fim de if
			} // Fim de try
			catch (IOException e) 
			{            
				Log.e(TAG, e.toString());
			} // Fim de catch
			finally // Garante que BluetoothServerSocket e BluetoothSocket serao fechados 
			{
				try 
				{
					// Se o BluetoothServerSocket não é nulo, fechá-lo 
					if (serverSocket != null)
						serverSocket.close();

					// Se o BluetoothSocket não é nulo, fechá-lo 
					if (socket != null)
						socket.close();
				} // Fim de try
				catch (IOException e) // Problema fechando um socket
				{
					Log.e(TAG, e.toString());
				} // Fim de catch
			} // Fim de finally

			return null;
		} // Fim do metodo doInBackround
	} // Fim de classe aninhada ReceiveContactTask 

	//  Handler usado para exibir um Toast em thread GUI com mensagem especificada
	public static void displayToastViaHandler(final Context context, 
			Handler handler, final int stringID)
	{
		handler.post(
				new Runnable()
				{
					public void run()
					{
						Toast.makeText(context, stringID,
								Toast.LENGTH_SHORT).show();
					} // Fim do metodo run
				} // Fim de Runnable
				); // Fim da chamada do metodo post do handler
	} // Fim do metodo displayToastViaHandler
} // Fim da classe AgendaTelefonica