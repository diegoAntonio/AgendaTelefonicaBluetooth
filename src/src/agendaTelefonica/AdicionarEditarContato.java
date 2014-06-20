// AdicionarEditarContato.java
// Activity para adicionar uma nova entrada ou
// Editar uma entrada existente na agenda telefonica.
package src.agendaTelefonica;

import com.agendaTelefonica.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AdicionarEditarContato extends Activity 
{
	private long rowID; // Id do contato que está sendo editado, se existir

	// EditTexts para informações de contato
	private EditText nameEditText;
	private EditText phoneEditText;
	private EditText emailEditText;
	private EditText streetEditText;
	private EditText cityEditText;

	// Chamado quando a atividade é iniciado pela primeira vez
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState); // Chama de super do onCreate
		setContentView(R.layout.add_contact); // Aumentar o UI

		nameEditText = (EditText) findViewById(R.id.nameEditText);
		emailEditText = (EditText) findViewById(R.id.emailEditText);
		phoneEditText = (EditText) findViewById(R.id.phoneEditText);
		streetEditText = (EditText) findViewById(R.id.streetEditText);
		cityEditText = (EditText) findViewById(R.id.cityEditText);

		Bundle extras = getIntent().getExtras(); // Obtém Pacote de extras 

		// Se existem extras, usá-los para preencher as EditTexts 
		if (extras != null)
		{
			rowID = extras.getLong("row_id");
			nameEditText.setText(extras.getString("name"));  
			emailEditText.setText(extras.getString("email"));  
			phoneEditText.setText(extras.getString("phone"));  
			streetEditText.setText(extras.getString("street"));  
			cityEditText.setText(extras.getString("city"));  
		} // Fim do if

		// Configura event listener para o botão Salvar contato
		Button saveContactButton = 
				(Button) findViewById(R.id.saveContactButton);
		saveContactButton.setOnClickListener(saveContactButtonClicked);
	} // Fim do metodo onCreate

	// Responde ao evento gerado quando o usuário clica no botão "Done"
	OnClickListener saveContactButtonClicked = new OnClickListener() 
	{
		@Override
		public void onClick(View v) 
		{
			if (nameEditText.getText().length() != 0)
			{
				AsyncTask<Object, Object, Object> saveContactTask = 
						new AsyncTask<Object, Object, Object>() 
						{
					@Override
					protected Object doInBackground(Object... params) 
					{
						saveContact(); // Salva o contato com o banco de dados
						return null;
					} // Fim do metodo doInBackground

					@Override
					protected void onPostExecute(Object result) 
					{
						finish(); // Retorno à Activity anterior
					} // Fim do metodo onPostExecute
						}; // Fim de AsyncTask

						// Salva o contato com o banco de dados usando um segmento separado
						saveContactTask.execute((Object[]) null); 
			} // Fim do if
			else
			{
				// Cria um novo AlertDialog Builder
				AlertDialog.Builder builder = 
						new AlertDialog.Builder(AdicionarEditarContato.this);

				// Define titulo de dialogo e mensagem, e fornecer Botao para descartar
				builder.setTitle(R.string.errorTitle); 
				builder.setMessage(R.string.errorMessage);
				builder.setPositiveButton(R.string.errorButton, null); 
				builder.show(); // Exibe a caixa de diálogo
			} // Fim do else
		} // Fim do metodo onClick
	}; // Fim de OnClickListener saveContactButtonClicked

	// Salva as informações de contato para o banco de dados
	private void saveContact() 
	{
		// Obtém DatabaseConnector para interagir com o banco de dados SQLite
		ConexaoBancoDados databaseConnector = new ConexaoBancoDados(this);

		if (getIntent().getExtras() == null)
		{
			// Inserir as informações de contato no banco de dados
			databaseConnector.insertContact(
					nameEditText.getText().toString(),
					emailEditText.getText().toString(), 
					phoneEditText.getText().toString(), 
					streetEditText.getText().toString(),
					cityEditText.getText().toString());
		} // Fim do if
		else
		{
			databaseConnector.updateContact(rowID,
					nameEditText.getText().toString(),
					emailEditText.getText().toString(), 
					phoneEditText.getText().toString(), 
					streetEditText.getText().toString(),
					cityEditText.getText().toString());
		} // Fim do else
	} // Fim da classe saveContact
} // Fim da classe AddEditContact