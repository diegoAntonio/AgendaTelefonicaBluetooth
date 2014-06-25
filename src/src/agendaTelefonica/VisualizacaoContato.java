// VisualizacaoContato.java
// activity para a visualiza��o de um �nico contato.
package src.agendaTelefonica;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.agendaTelefonica.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class VisualizacaoContato extends Activity 
{
	private static final String TAG = VisualizacaoContato.class.getName();

	// C�digo de pedido Intent usado para iniciar uma activity que retorna um resultado
	private static final int REQUEST_CONNECT_DEVICE = 1;

	private BluetoothAdapter bluetoothAdapter = null; // Declaracao de uma variavel BluetoothAdapter
	private Handler handler; // Handler para a exibi��o de Toasts no segmento de GUI 
	private long rowID; // ID do contato selecionado na linha de identifica��o do Banco de Dados 
	private TextView nameTextView; // Exibe o nome do contato
	private TextView phoneTextView; // Exibe o telefone de contato 
	private TextView emailTextView; // Exibe um contato de e-mail 
	private TextView streetTextView; // Exibe rua do contato
	private TextView cityTextView; // Exibe cidade/estado/CEP do contato

	// Chamado quando a activity � criada pela primeira vez
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_contact); // inflate GUI

		// Obt�m os EditTexts
		nameTextView = (TextView) findViewById(R.id.nameTextView);
		phoneTextView = (TextView) findViewById(R.id.phoneTextView);
		emailTextView = (TextView) findViewById(R.id.emailTextView);
		streetTextView = (TextView) findViewById(R.id.streetTextView);
		cityTextView = (TextView) findViewById(R.id.cityTextView);

		// Obt�m o contato selecionado linha ID
		Bundle extras = getIntent().getExtras();
		rowID = extras.getLong(AgendaTelefonica.ROW_ID); 

		// Obt�m o adaptador Bluetooth padr�o
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		handler = new Handler(); // Criacao do Handler
	} // Fim do metodo onCreate

	// Chamado quando a activity � criada pela primeira vez
	@Override
	protected void onResume()
	{
		super.onResume();

		// Cria novo LoadContactTask e o executa
		new LoadContactTask().execute(rowID);
	} // Fim do metodo onResume

	// Realiza consulta de banco de dados fora do thread de GUI
	private class LoadContactTask extends AsyncTask<Long, Object, Cursor> 
	{
		ConexaoBancoDados databaseConnector = 
				new ConexaoBancoDados(VisualizacaoContato.this);

		// Executar o acesso ao banco
		@Override
		protected Cursor doInBackground(Long... params)
		{
			databaseConnector.open();

			// Obt�m um cursor que cont�m todos os dados sobre determinada entrada
			return databaseConnector.getOneContact(params[0]);
		} // Fim do metodo doInBackground

		// Usar o cursor retornado do m�todo doInBackground
		@Override
		protected void onPostExecute(Cursor result)
		{
			super.onPostExecute(result);

			result.moveToFirst(); // Mover para o primeiro item

			// Obt�m o �ndice da coluna para cada item de dados
			int nameIndex = result.getColumnIndex("name");
			int phoneIndex = result.getColumnIndex("phone");
			int emailIndex = result.getColumnIndex("email");
			int streetIndex = result.getColumnIndex("street");
			int cityIndex = result.getColumnIndex("city");

			// Preenchendo TextViews com os dados recuperados
			nameTextView.setText(result.getString(nameIndex));
			phoneTextView.setText(result.getString(phoneIndex));
			emailTextView.setText(result.getString(emailIndex));
			streetTextView.setText(result.getString(streetIndex));
			cityTextView.setText(result.getString(cityIndex));

			result.close(); // Fecha o result
			databaseConnector.close(); // Fecha a conex�o com o banco
		} // Fim do metodo onPostExecute
	} // Fim da classe LoadContactTask

	// Cria o menu da activity de um arquivo XML recurso de menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_contact_menu, menu);
		return true;
	} // Fim do metodo onCreateOptionsMenu

	// Controle de Escolha de menu de op��es
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) // Switch baseado no ID selecionado do MenuItem
		{
		case R.id.editItem: // Selecionado Editar Contato item de menu
			// Cria a Intent de iniciar o AddEditContact activity
			Intent addEditContact =
			new Intent(this, AdicionarEditarContato.class);

			// Passa os dados do contato selecionado como extras, com a Intent
			addEditContact.putExtra(AgendaTelefonica.ROW_ID, rowID);
			addEditContact.putExtra("name", nameTextView.getText());
			addEditContact.putExtra("phone", phoneTextView.getText());
			addEditContact.putExtra("email", emailTextView.getText());
			addEditContact.putExtra("street", streetTextView.getText());
			addEditContact.putExtra("city", cityTextView.getText());
			startActivity(addEditContact); // Inicia o Activity
			break;
		case R.id.deleteItem: // Selecionado Excluir contato item de menu
			deleteContact(); // Exclui o contato exibido
			break;
		case R.id.transferItem: // Selecionado Transfer�ncia contato item de menu
			// Se n�o estiver conectado
			if (bluetoothAdapter.isEnabled()) 
			{
				// Inicia DeviceChooser assim o usu�rio pode escolher um dispositivo pr�ximo
				Intent serverIntent = 
						new Intent(this, SeletorDispositivo.class);
				startActivityForResult(
						serverIntent, REQUEST_CONNECT_DEVICE);
			} // Fim do if
			else // Indica que o Bluetooth n�o est� habilitado
			{
				Toast.makeText(this, 
						R.string.no_bluetooth, Toast.LENGTH_LONG).show();
			} // Fim do else
			break;
		} // Fim do switch

		return super.onOptionsItemSelected(item);
	} // Fim do m�todo onOptionsItemSelected

	// Apaga um contato
	private void deleteContact()
	{
		// Cria um novo AlertDialog Builder
		AlertDialog.Builder builder = 
				new AlertDialog.Builder(VisualizacaoContato.this);

		builder.setTitle(R.string.confirmTitle); // Barra de titulo String
		builder.setMessage(R.string.confirmMessage); // Mensagem para ser exibida

		// Fornece um bot�o OK que descarta o di�logo
		builder.setPositiveButton(R.string.button_delete,
				new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int button)
			{
				final ConexaoBancoDados databaseConnector = 
						new ConexaoBancoDados(VisualizacaoContato.this);
				
				// Cria um AsyncTask que exclui o contato em outro
				// Thread, em seguida, chama de chegada ap�s a elimina��o
				AsyncTask<Long, Object, Object> deleteTask =
						new AsyncTask<Long, Object, Object>()
						{
					@Override
					protected Object doInBackground(Long... params)
					{
						databaseConnector.deleteContact(params[0]); 
						return null;
					} // Fim do metodo doInBackground

					@Override
					protected void onPostExecute(Object result)
					{
						finish(); // Retorno ao AgendaTelefonica Activity
					} // Fim do metodo onPostExecute
						}; // Fim do novo AsyncTask
						
						// Executa o AsyncTask excluir contato na rowID
						deleteTask.execute(new Long[] { rowID });               
			} // Fim do m�todo onClick
		} // Fim da classe interna an�nima
				); // Fim para o m�todo setPositiveButton

		builder.setNegativeButton(R.string.button_cancel, null);
		builder.show(); // Exibe a caixa de di�logo
	} // Fim do metodo DeleteContact

	// Chamado quando uma activity � iniciada a partir deste
	// Termina startActivityForResult
	@SuppressLint("ShowToast")
	public void onActivityResult(int requestCode, int resultCode, 
			Intent data) 
	{
		// Se a conex�o foi estabelecida
		if (resultCode == Activity.RESULT_OK) 
		{
			// Obt�m o endere�o MAC do dispositivo remoto e o passa para que 
			// SendContactTask do m�todo execute
			new SendContactTask().execute(new String[] { 
					data.getExtras().getString(SeletorDispositivo.DEVICE_ADDRESS)});
		} // Fim do if
		else // Houve um erro de conex�o
		{
			Toast.makeText(this, 
					R.string.connection_error, Toast.LENGTH_LONG);
		} // end else
	} // Fim do metodo onActivityResult

	// Tarefa para o envio de um contato em uma discuss�o de fundo
	private class SendContactTask extends AsyncTask<String, Object, Object>
	{
		// Obt�m o BluetoothDevice para o endere�o especificado,
		// Conecta ao dispositivo e enviar o contato
		@Override
		protected Object doInBackground(String... params)
		{
			// Obt�m um objeto BluetoothDevice representando o dispositivo remoto
			BluetoothDevice device =
					bluetoothAdapter.getRemoteDevice(params[0]);

			BluetoothSocket bluetoothSocket = null; // Para o envio de contato

			// Fazer a conex�o ao dispositivo remoto e enviar contato
			try 
			{
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.sending_contact);

				// Obt�m BluetoothSocket, em seguida, ligue para o outro dispositivo
				bluetoothSocket = device.createRfcommSocketToServiceRecord(
						AgendaTelefonica.MY_UUID);
				bluetoothSocket.connect(); // Estabelecer a conex�o

				// Obt�m fluxos de comunica��o via BluetoothSocket
				OutputStream outputStream = bluetoothSocket.getOutputStream();
				
				// Cria JSONObject representando o contato
				final JSONObject contact = new JSONObject();
				contact.put("name", nameTextView.getText().toString());
				contact.put("phone", phoneTextView.getText().toString());
				contact.put("email", emailTextView.getText().toString());
				contact.put("street", streetTextView.getText().toString());
				contact.put("city", cityTextView.getText().toString());

				// Envia uma matriz de bytes que cont�m as informa��es do contato
				outputStream.write(contact.toString().getBytes()); 
				outputStream.flush();
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.contact_sent);
			} // Fim do try
			catch (IOException e) // Problema enviando contato
			{
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.transfer_failed);
				Log.e(TAG, e.toString());
			} // Fim do catch
			catch (JSONException e) // Problema com formato de arquivo JSON
			{
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.transfer_failed);
				Log.e(TAG, e.toString());
			} // Fim do catch
			finally // Garantir que BluetoothSocket est� fechado
			{
				try
				{
					bluetoothSocket.close(); // Fecha BluetoothSocket
				} // Fim do try
				catch (IOException e) // Problema fechando BluetoothSocket
				{
					Log.e(TAG, e.toString());
				} // Fim do catch

				bluetoothSocket = null;
			} // Fim do finally

			return null;
		} // Fim do metodo doInBackground
	} // Fim da classe SendContactTask 
} // Fim da classe ViewContact